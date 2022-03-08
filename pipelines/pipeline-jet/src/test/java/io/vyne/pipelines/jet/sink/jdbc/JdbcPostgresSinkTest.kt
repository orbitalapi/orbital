package io.vyne.pipelines.jet.sink.jdbc

import com.hazelcast.jet.pipeline.test.TestSources
import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.PostgresSQLContainerFacade
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.ItemStreamSourceSpec
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import org.awaitility.Awaitility
import org.jooq.DSLContext
import org.jooq.impl.DSL.condition
import org.jooq.impl.DSL.table
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import java.util.Queue


@Testcontainers
@RunWith(SpringRunner::class)
class JdbcPostgresSinkTest : BaseJetIntegrationTest() {
   lateinit var postgresSQLContainerFacade: PostgresSQLContainerFacade

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @Before
   fun before() {
      postgresSQLContainerFacade = PostgresSQLContainerFacade(postgreSQLContainer)
      postgresSQLContainerFacade.start()
   }


   @Test
   fun `can stream large records to postgres`() {
      val schemaSource = """
         model Person {
            @Id
            id : PersonId inherits Int by column(1)
            firstName : FirstName inherits String by column(2)
            lastName : LastName inherits String by column(3)
         }
      """
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )

      // Register the connection so we can look it up later
      val connectionRegistry = applicationContext.getBean(JdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)

      val vyne = vyneProvider.createVyne()
      val src = "123,Jimmy,Popps"
      val typedInstance = TypedInstance.from(vyne.type("Person"), src, vyne.schema)
      // A stream that generates 100 items per second
      val stream = TestSources.itemStream(1000) { timestamp: Long, sequence: Long ->
         StringContentProvider("$sequence,Jimmy $sequence,Smitts")
      }
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = ItemStreamSourceSpec(
            source = stream,
            typeName = "Person".fqn()
         ),
         output = JdbcTransportOutputSpec(
            "test-connection",
            emptyMap(),
            "Person"
         )
      )
      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)

      val connectionFactory = applicationContext.getBean(JdbcConnectionFactory::class.java)
      // We're emitting 1000 messages a second.  If we haven't completed within 10 seconds, we're lagging too much.
      val startTime = Instant.ofEpochMilli(job.submissionTime)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), vyne.type("Person"), 5000, startTime, Duration.ofSeconds(10L))
   }

   @Test
   fun `can upsert to change column values`() {
      val schemaSource = """
         model Person {
            @Id
            id : PersonId inherits Int by column(1)
            firstName : FirstName inherits String by column(2)
            lastName : LastName inherits String by column(3)
         }
      """
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )

      // Register the connection so we can look it up later
      val connectionRegistry = applicationContext.getBean(JdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)

      val vyne = vyneProvider.createVyne()
      fun buildPipelineSpec(items: Queue<String>) = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = items,
            typeName = "Person".fqn()
         ),
         output = JdbcTransportOutputSpec(
            "test-connection",
            emptyMap(),
            "Person"
         )
      )

      val firstPipelineSpec = buildPipelineSpec(queueOf("123,Jimmy,Popps"))
      val (_, firstJob) = startPipeline(jetInstance, vyneProvider, firstPipelineSpec)

      val connectionFactory = applicationContext.getBean(JdbcConnectionFactory::class.java)
      val type = vyne.type("Person")
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), type, 1)
      firstJob.cancel()

      // Now spin up a second pipeline to generate the update

      val secondPipelineSpec = buildPipelineSpec(queueOf("123,Jimmy,Poopyface", "456,Jenny,Poops"))
      val (_, secondJob) = startPipeline(jetInstance, vyneProvider, secondPipelineSpec)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), type, 2)

      Awaitility.await().atMost(Duration.ofSeconds(5)).until {
         val upsertedRecord = connectionFactory.dsl(postgresSQLContainerFacade.connection)
            .selectFrom(SqlUtils.tableNameOrTypeName(type.taxiType))
            .where(condition("id = 123"))
            .fetch()
            .single()
         upsertedRecord["lastname"] == "Poopyface"
      }

   }


   @Test
   fun canOutputToJdbc() {
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val vyne = vyneProvider.createVyne()
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = JdbcTransportOutputSpec(
            "test-connection",
            emptyMap(),
            "Target"
         )
      )
      val connectionFactory = applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Table shouldn't exist
      val startRowCount = rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), vyne.type("Target"))
      startRowCount.should.equal(-1)

      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)

      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), vyne.type("Target"), 1)
   }

   private fun waitForRowCount(
      dsl: DSLContext,
      type: Type,
      rowCount: Int,
      startTime: Instant = Instant.now(),
      duration: Duration = Duration.ofSeconds(10)
   ) {
      Awaitility.await().atMost(duration)
         .until {
            val currentRowCount = rowCount(dsl, type)
            logger.info(
               "Row count after ${
                  Duration.between(startTime, Instant.now()).toMillis()
               }ms is $currentRowCount (Waiting until it hits $rowCount)"
            )
            currentRowCount >= rowCount
         }
   }

   private fun rowCount(dsl: DSLContext, type: Type): Int {
      return try {
         dsl.fetchCount(
            table(SqlUtils.tableNameOrTypeName(type.taxiType))
         )
      } catch (e: Exception) {
         -1
      } // return -1 if the table doesn't exist
   }
}
