package com.orbitalhq.pipelines.jet

import com.hazelcast.config.Config
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.jet.core.JetTestSupport
import com.hazelcast.jet.core.JobStatus
import com.hazelcast.spring.context.SpringManagedContext
import com.mercateo.test.clock.TestClock
import com.nhaarman.mockito_kotlin.mock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import com.orbitalhq.*
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.InMemoryKafkaConnectorRegistry
import com.orbitalhq.embedded.EmbeddedVyneClientWithSchema
import com.orbitalhq.pipelines.jet.api.SubmittedPipeline
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineFactory
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.pipelines.jet.sink.PipelineSinkProvider
import com.orbitalhq.pipelines.jet.sink.list.ListSinkSpec
import com.orbitalhq.pipelines.jet.sink.list.ListSinkTarget
import com.orbitalhq.pipelines.jet.sink.list.ListSinkTargetContainer
import com.orbitalhq.pipelines.jet.sink.stream.StreamSinkSpec
import com.orbitalhq.pipelines.jet.sink.stream.StreamSinkTarget
import com.orbitalhq.pipelines.jet.sink.stream.StreamSinkTargetContainer
import com.orbitalhq.pipelines.jet.source.PipelineSourceProvider
import com.orbitalhq.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.SimpleVyneProvider
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.spring.invokers.RestTemplateInvoker
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.*
import java.util.function.Supplier

data class JetTestSetup(
   val hazelcastInstance: HazelcastInstance,
   val applicationContext: ApplicationContext,
   val vyneClient: VyneClientWithSchema,
   val stubService: StubService,
   val schema: Schema,
   val meterRegistry: MeterRegistry
)

abstract class BaseJetIntegrationTest : JetTestSupport() {
   val kafkaConnectionRegistry = InMemoryKafkaConnectorRegistry()
   val awsConnectionRegistry = AwsInMemoryConnectionRegistry()
   val pipelineSourceProvider = PipelineSourceProvider.default(kafkaConnectionRegistry)
   val pipelineSinkProvider = PipelineSinkProvider.default(kafkaConnectionRegistry, awsConnectionRegistry)
   val meterRegistry = SimpleMeterRegistry()


   fun jetWithSpringAndVyne(
       schema: String,
       jdbcConnections: List<JdbcConnectionConfiguration> = emptyList(),
       awsConnections: List<AwsConnectionConfiguration> = emptyList(),
       testClockConfiguration: Class<*> = TestClockProvider::class.java,
       contextConfig: (GenericApplicationContext) -> Unit = {},
   ): JetTestSetup {
      val (vyne, stub) = testVyneWithStub(schema) { taxiSchema ->
         listOf(
            CacheAwareOperationInvocationDecorator(
               RestTemplateInvoker(
                  SimpleSchemaProvider(taxiSchema),
                  WebClient.builder(),
                  AuthWebClientCustomizer.empty()
               )
            )
         )
      }

      val springApplicationContext = AnnotationConfigApplicationContext()
      springApplicationContext.registerBean(SimpleVyneProvider::class.java, vyne)
      springApplicationContext.registerBean(Schema::class.java, Supplier { vyne.schema })
      springApplicationContext.registerBean(SimpleSchemaStore::class.java, SchemaSet.from(vyne.schema, 1))
      springApplicationContext.register(EmbeddedVyneClientWithSchema::class.java)
      springApplicationContext.registerBean(
         "jdbcConnectionRegistry",
         InMemoryJdbcConnectionRegistry::class.java,
         jdbcConnections
      )
      springApplicationContext.registerBean(
         "awsConnectionRegistry",
         AwsInMemoryConnectionRegistry::class.java,
         awsConnections
      )

      springApplicationContext.register(testClockConfiguration)
      springApplicationContext.register(TestPipelineStateConfig::class.java)
      springApplicationContext.register(SimpleMeterRegistry::class.java)

      // For some reason, spring is complaining if we try to use a no-arg constructor
      springApplicationContext.registerBean(ListSinkTargetContainer.NAME, ListSinkTargetContainer::class.java, "Hello")
      springApplicationContext.registerBean(
         StreamSinkTargetContainer.NAME,
         StreamSinkTargetContainer::class.java,
         "Hello"
      )

      contextConfig.invoke(springApplicationContext)
      springApplicationContext.refresh()

      val hazelcastConfig = Config()
      hazelcastConfig.jetConfig.isEnabled = true
      hazelcastConfig.managedContext = SpringManagedContext(springApplicationContext)
      val hazelcastInstance = createHazelcastInstance(hazelcastConfig)
      val vyneClient = springApplicationContext.getBean(VyneClientWithSchema::class.java)
      val schema = springApplicationContext.getBean(Schema::class.java)
      val meterRegistry = springApplicationContext.getBean(MeterRegistry::class.java)
      return JetTestSetup(hazelcastInstance, springApplicationContext, vyneClient, stub, schema, meterRegistry)
   }

   /**
    * Builds a spring context containing a real vyne instance (that invokes actual services),
    * wired into a jet instance
    */
   fun jetWithSpringAndVyne(
       schema: TaxiSchema,
       jdbcConnections: List<JdbcConnectionConfiguration>,
       awsConnections: List<AwsConnectionConfiguration> = emptyList(),
       testClockConfiguration: Class<*> = TestClockProvider::class.java,
       contextConfig: (GenericApplicationContext) -> Unit = {},
   ): Triple<HazelcastInstance, ApplicationContext, VyneClient> {
      val vyne = testVyne(
         schema, listOf(
            CacheAwareOperationInvocationDecorator(
               RestTemplateInvoker(
                  SimpleSchemaProvider(schema),
                  WebClient.builder(),
                  AuthWebClientCustomizer.empty()
               )
            )
         )
      )

      val springApplicationContext = AnnotationConfigApplicationContext()
      springApplicationContext.registerBean(SimpleVyneProvider::class.java, vyne)
      springApplicationContext.registerBean(
         "jdbcConnectionRegistry",
         InMemoryJdbcConnectionRegistry::class.java,
         jdbcConnections
      )
      springApplicationContext.registerBean(
         "awsConnectionRegistry",
         AwsInMemoryConnectionRegistry::class.java,
         awsConnections
      )
      springApplicationContext.register(testClockConfiguration)
      springApplicationContext.register(TestPipelineStateConfig::class.java)

      // For some reason, spring is complaining if we try to use a no-arg constructor
      springApplicationContext.registerBean(ListSinkTargetContainer.NAME, ListSinkTarget::class.java, "Hello")

      contextConfig.invoke(springApplicationContext)
      springApplicationContext.refresh()

      val hazelcastConfig = Config()
      hazelcastConfig.managedContext = SpringManagedContext(springApplicationContext)
      val hazelcastInstance = createHazelcastInstance(hazelcastConfig)
      val vyneClient = springApplicationContext.getBean(VyneClient::class.java)
      return Triple(hazelcastInstance, springApplicationContext, vyneClient)
   }

   fun listSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: String,
      name: String = "default"
   ): Pair<ListSinkTarget, ListSinkSpec> {
      return listSinkTargetAndSpec(applicationContext, targetType.fqn(), name)
   }

   fun listSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: QualifiedName,
      name: String = "default"
   ): Pair<ListSinkTarget, ListSinkSpec> {
      val listSinkTargetContainer = applicationContext.getBean(ListSinkTargetContainer::class.java)
      return listSinkTargetContainer.getOrCreateTarget(name) to ListSinkSpec(targetType, name)
   }

   fun streamSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: String,
      name: String = "default"
   ): Pair<StreamSinkTarget, StreamSinkSpec> {
      return streamSinkTargetAndSpec(applicationContext, targetType.fqn(), name)
   }

   fun streamSinkTargetAndSpec(
      applicationContext: ApplicationContext,
      targetType: QualifiedName,
      name: String = "default"
   ): Pair<StreamSinkTarget, StreamSinkSpec> {
      val streamSinkTargetContainer = applicationContext.getBean(StreamSinkTargetContainer::class.java)
      return streamSinkTargetContainer.getOrCreateTarget(name) to StreamSinkSpec(targetType, name)
   }

   fun startPipeline(
      hazelcastInstance: HazelcastInstance,
      vyneClient: VyneClientWithSchema,
      pipelineSpec: PipelineSpec<*, *>,
      sourceProvider: PipelineSourceProvider = pipelineSourceProvider,
      sinkProvider: PipelineSinkProvider = pipelineSinkProvider,
      validateJobStatusIsRunningEventually: Boolean = true
   ): Triple<SubmittedPipeline, Job?, PipelineManager> {
      val manager = pipelineManager(hazelcastInstance, vyneClient, sourceProvider, sinkProvider)
      Timer().scheduleAtFixedRate(
         object : TimerTask() {
            override fun run() {
               manager.runScheduledPipelinesIfAny()
            }
         },
         0, 1000
      )
      val (pipeline, job) = manager.startPipeline(
         pipelineSpec
      )

      if (job != null && validateJobStatusIsRunningEventually) {
         assertJobStatusEventually(job, JobStatus.RUNNING, 5)
      }

      return Triple(pipeline, job, manager)
   }

   fun pipelineManager(
      hazelcastInstance: HazelcastInstance,
      vyneClient: VyneClientWithSchema,
      sourceProvider: PipelineSourceProvider = pipelineSourceProvider,
      sinkProvider: PipelineSinkProvider = pipelineSinkProvider,
   ): PipelineManager {
      return PipelineManager(
         PipelineFactory(vyneClient, sourceProvider, sinkProvider),
         hazelcastInstance
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

