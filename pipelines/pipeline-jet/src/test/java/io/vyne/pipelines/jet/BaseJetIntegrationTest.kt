package io.vyne.pipelines.jet

import com.hazelcast.config.Config
import com.hazelcast.jet.JetInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.config.JetConfig
import com.hazelcast.jet.core.JetTestSupport
import com.hazelcast.jet.core.JobStatus
import com.hazelcast.spring.context.SpringManagedContext
import com.mercateo.test.clock.TestClock
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import io.vyne.connectors.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.pipelines.jet.api.SubmittedPipeline
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.pipelines.PipelineFactory
import io.vyne.pipelines.jet.pipelines.PipelineManager
import io.vyne.pipelines.jet.sink.PipelineSinkProvider
import io.vyne.pipelines.jet.sink.list.ListSinkSpec
import io.vyne.pipelines.jet.sink.list.ListSinkTarget
import io.vyne.pipelines.jet.source.PipelineSourceProvider
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleVyneProvider
import io.vyne.spring.VyneProvider
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.testVyne
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

abstract class BaseJetIntegrationTest : JetTestSupport() {

   fun jetWithSpringAndVyne(
      schema: String,
      jdbcConnections:List<JdbcConnectionConfiguration>,
      awsConnections: List<AwsConnectionConfiguration> = emptyList(),
      testClockConfiguration: Class<*> = TestClockProvider::class.java,
      contextConfig: (GenericApplicationContext) -> Unit = {},
   ): Triple<JetInstance, ApplicationContext, VyneProvider> {
      val vyne = testVyne(schema) { taxiSchema ->
         listOf(
            CacheAwareOperationInvocationDecorator(
               RestTemplateInvoker(
                  SimpleSchemaStore().setSchemaSet(SchemaSet.from(taxiSchema.sources, 1)),
                  WebClient.builder(),
                  ServiceUrlResolver.DEFAULT
               )
            )
         )
      }

      val springApplicationContext = AnnotationConfigApplicationContext()
      springApplicationContext.registerBean(SimpleVyneProvider::class.java, vyne)
      springApplicationContext.registerBean(
         "jdbcConnectionRegistry",
         InMemoryJdbcConnectionRegistry::class.java,
         jdbcConnections
      )
      springApplicationContext.registerBean("awsConnectionRegistry",
         AwsInMemoryConnectionRegistry::class.java,
         awsConnections
      )

      springApplicationContext.register(testClockConfiguration)
      springApplicationContext.register(TestPipelineStateConfig::class.java)

      // For some reason, spring is complaining if we try to use a no-arg constructor
      springApplicationContext.registerBean(ListSinkTarget.NAME, ListSinkTarget::class.java, "Hello")

      contextConfig.invoke(springApplicationContext)
      springApplicationContext.refresh()

      val jetConfig = JetConfig() // configure SpringManagedContext for @SpringAware
         .configureHazelcast { hzConfig: Config ->
            hzConfig.managedContext = SpringManagedContext(springApplicationContext)
         }

      val jetInstance = createJetMember(jetConfig)
      val vyneProvider = springApplicationContext.getBean(VyneProvider::class.java)
      return Triple(jetInstance, springApplicationContext, vyneProvider)
   }
   /**
    * Builds a spring context containing a real vyne instance (that invokes actual services),
    * wired into a jet instance
    */
   fun jetWithSpringAndVyne(
      schema: TaxiSchema,
      jdbcConnections:List<JdbcConnectionConfiguration>,
      awsConnections: List<AwsConnectionConfiguration> = emptyList(),
      testClockConfiguration: Class<*> = TestClockProvider::class.java,
      contextConfig: (GenericApplicationContext) -> Unit = {},
   ): Triple<JetInstance, ApplicationContext, VyneProvider> {
      val vyne = testVyne(schema, listOf(
         CacheAwareOperationInvocationDecorator(
            RestTemplateInvoker(
               SimpleSchemaStore().setSchemaSet(SchemaSet.from(schema.sources, 1)),
               WebClient.builder(),
               ServiceUrlResolver.DEFAULT
            )
         )
      ))

      val springApplicationContext = AnnotationConfigApplicationContext()
      springApplicationContext.registerBean(SimpleVyneProvider::class.java, vyne)
      springApplicationContext.registerBean(
         "jdbcConnectionRegistry",
         InMemoryJdbcConnectionRegistry::class.java,
         jdbcConnections
      )
      springApplicationContext.registerBean("awsConnectionRegistry",
      AwsInMemoryConnectionRegistry::class.java,
         awsConnections
      )
      springApplicationContext.register(testClockConfiguration)
      springApplicationContext.register(TestPipelineStateConfig::class.java)

      // For some reason, spring is complaining if we try to use a no-arg constructor
      springApplicationContext.registerBean(ListSinkTarget.NAME, ListSinkTarget::class.java, "Hello")

      contextConfig.invoke(springApplicationContext)
      springApplicationContext.refresh()

      val jetConfig = JetConfig() // configure SpringManagedContext for @SpringAware
         .configureHazelcast { hzConfig: Config ->
            hzConfig.managedContext = SpringManagedContext(springApplicationContext)
         }

      val jetInstance = createJetMember(jetConfig)
      val vyneProvider = springApplicationContext.getBean(VyneProvider::class.java)
      return Triple(jetInstance, springApplicationContext, vyneProvider)
   }

   fun listSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: String
   ): Pair<ListSinkTarget, ListSinkSpec> {
      return listSinkTargetAndSpec(applicationContext, targetType.fqn())
   }

   fun listSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: QualifiedName
   ): Pair<ListSinkTarget, ListSinkSpec> {
      val listSinkTarget = applicationContext.getBean(ListSinkTarget::class.java)
      return listSinkTarget to ListSinkSpec(targetType)
   }

   fun startPipeline(
      jetInstance: JetInstance,
      vyneProvider: VyneProvider,
      pipelineSpec: PipelineSpec<*, *>,
      sourceProvider: PipelineSourceProvider = PipelineSourceProvider.default(),
      sinkProvider: PipelineSinkProvider = PipelineSinkProvider.default(),
      validateJobStatusEventually: Boolean = true
   ): Pair<SubmittedPipeline, Job> {
      val manager = pipelineManager(jetInstance, vyneProvider, sourceProvider, sinkProvider)
      val (pipeline, job) = manager.startPipeline(
         pipelineSpec
      )

      if (validateJobStatusEventually) {
         assertJobStatusEventually(job, JobStatus.RUNNING, 5)
      }

      return pipeline to job
   }

   fun pipelineManager(
      jetInstance: JetInstance,
      vyneProvider: VyneProvider,
      sourceProvider: PipelineSourceProvider = PipelineSourceProvider.default(),
      sinkProvider: PipelineSinkProvider = PipelineSinkProvider.default(),
   ): PipelineManager {
      return PipelineManager(
         PipelineFactory(vyneProvider, sourceProvider, sinkProvider),
         jetInstance

      )
   }

   fun ApplicationContext.moveTimeForward(seconds: Int) = moveTimeForward(Duration.ofSeconds(seconds.toLong()))
   fun ApplicationContext.moveTimeForward(duration: Duration) {
      val clock = this.testClock()
      clock.fastForward(duration)
      logger.info("Time moved forward ${duration.seconds}s, now ${clock.instant()}")
   }


   fun ApplicationContext.testClock(): TestClock {
      return this.getBean(TestClock::class.java)
   }
}

