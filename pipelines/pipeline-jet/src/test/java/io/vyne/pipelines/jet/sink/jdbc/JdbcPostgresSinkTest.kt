package io.vyne.pipelines.jet.sink.jdbc

import com.hazelcast.jet.impl.connector.WriteBufferedP
import com.hazelcast.jet.pipeline.test.TestSources
import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.models.json.parseJson
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.PostgresSQLContainerFacade
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.jdbc.WriteDisposition
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.BatchItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.fixed.ItemStreamSourceSpec
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import nl.altindag.log.LogCaptor
import org.awaitility.Awaitility
import org.jooq.DSLContext
import org.jooq.impl.DSL.condition
import org.jooq.impl.DSL.table
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Disabled
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


@Disabled("Flakey - breaking the build")
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
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )

      // Register the connection so we can look it up later
      val connectionRegistry = testSetup.applicationContext.getBean(InMemoryJdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)

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
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Person"
            )
         )
      )
      val (_, job) = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)
      // We're emitting 1000 messages a second.  If we haven't completed within 10 seconds, we're lagging too much.
      val startTime = Instant.ofEpochMilli(job!!.submissionTime)
      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         testSetup.schema.type("Person"),
         5000,
         startTime
      )
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
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )

      // Register the connection so we can look it up later
      val connectionRegistry = testSetup.applicationContext.getBean(InMemoryJdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)

      fun buildPipelineSpec(items: Queue<String>) = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = items,
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Person"
            )
         )
      )

      val firstPipelineSpec = buildPipelineSpec(queueOf("123,Jimmy,Popps"))
      val (_, firstJob) = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, firstPipelineSpec)

      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)
      val type = testSetup.schema.type("Person")
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), type, 1)
      firstJob!!.cancel()

      // Now spin up a second pipeline to generate the update

      val secondPipelineSpec = buildPipelineSpec(queueOf("123,Jimmy,Poopyface", "456,Jenny,Poops"))
      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, secondPipelineSpec)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), type, 2)

      Awaitility.await().atMost(Duration.ofSeconds(5)).until {
         val upsertedRecord = connectionFactory.dsl(postgresSQLContainerFacade.connection)
            .selectFrom(SqlUtils.tableNameOrTypeName(type.taxiType))
            .where(condition("id = 123"))
            .fetch()
            .single()
         upsertedRecord["lastName"] == "Poopyface"
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
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Target"
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Table shouldn't exist
      val startRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"))
      startRowCount.should.equal(-1)

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"), 1)
   }


   @Test
   fun canHandleBatchSource() {
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = BatchItemsSourceSpec(
            items = listOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn(),
            groupId = "test"
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Target",
               WriteDisposition.RECREATE
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Table shouldn't exist
      val startRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"))
      startRowCount.should.equal(-1)

      startPipeline(
         testSetup.hazelcastInstance,
         testSetup.vyneClient,
         pipelineSpec,
         validateJobStatusIsRunningEventually = false
      )

      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         testSetup.schema.type("Target"),
         1,
         duration = Duration.ofSeconds(30L)
      )
   }

   @Test
   fun canHandleEmptyBatchSource() {
      val jdbcSinkBuilderLogCaptor = LogCaptor.forClass(WriteBufferedP::class.java)
      jdbcSinkBuilderLogCaptor.setLogLevelToInfo()
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = BatchItemsSourceSpec(
            items = emptyList(),
            typeName = "Person".fqn(),
            groupId = "test"
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Target",
               WriteDisposition.RECREATE
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Table shouldn't exist
      val startRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"))
      startRowCount.should.equal(-1)

      startPipeline(
         testSetup.hazelcastInstance,
         testSetup.vyneClient,
         pipelineSpec,
         validateJobStatusIsRunningEventually = false
      )
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until {
         jdbcSinkBuilderLogCaptor.infoLogs.any { traceLog ->
            traceLog.contains("Not updating the DB view for Target as there was no data received, and as such no table was created.")
         }
      }
   }

   @Test
   fun truncateModeWorks() {
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val outputs = listOf(
         JdbcTransportOutputSpec(
            "test-connection",
            "Target",
            WriteDisposition.RECREATE
         )
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = BatchItemsSourceSpec(
            items = listOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn(),
            groupId = "1"
         ),
         outputs = outputs
      )
      val pipelineSpec2 = PipelineSpec(
         name = "test-http-poll",
         input = BatchItemsSourceSpec(
            items = listOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn(),
            groupId = "2"
         ),
         outputs = outputs
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Underlying tables shouldn't exist
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_1", false)
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_2", false)

      startPipeline(
         testSetup.hazelcastInstance,
         testSetup.vyneClient,
         pipelineSpec,
         validateJobStatusIsRunningEventually = false
      )
      // The first table should get created
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_1", true)
      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         testSetup.schema.type("Target"),
         1,
         duration = Duration.ofSeconds(30L)
      )


      startPipeline(
         testSetup.hazelcastInstance,
         testSetup.vyneClient,
         pipelineSpec2,
         validateJobStatusIsRunningEventually = false
      )

      // The second table should get created
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_2", true)

      // The table for the first group id should be deleted once everything is completed
      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         testSetup.schema.type("Target"),
         1,
         duration = Duration.ofSeconds(30L)
      )
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_1", true)
      waitForTableExistence(connectionFactory.dsl(postgresSQLContainerFacade.connection), "target_2", true)
   }

   @Test
   fun canHaveMultipleTablesInJdbc() {
      val schemaSource = """
         model Person {
            @Id()
            id : Id inherits String
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            @Id()
            id : Id inherits String
            givenName : FirstName
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val personPipelineSpec = PipelineSpec(
         name = "test-person",
         input = FixedItemsSourceSpec(
            items = queueOf(),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Person"
            )
         )
      )
      val targetPipelineSpec = PipelineSpec(
         name = "test-target",
         input = FixedItemsSourceSpec(
            items = queueOf(),
            typeName = "Target".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Target"
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Tables shouldn't exist
      val personStartRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Person"))
      personStartRowCount.should.equal(-1)
      val targetStartRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"))
      targetStartRowCount.should.equal(-1)

      val (_, _) = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, personPipelineSpec)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Person"), 0)

      val (_, _) = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, targetPipelineSpec)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Target"), 0)
   }

   @Test
   fun canHaveTableWithJustIdInJdbc() {
      val schemaSource = """
         model Person {
            @Id()
            id : Id inherits String
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(postgresSQLContainerFacade.connection)
      )
      val personPipelineSpec = PipelineSpec(
         name = "test-person",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "id" : "abcdefg" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Person"
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      // Tables shouldn't exist
      val personStartRowCount =
         rowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Person"))
      personStartRowCount.should.equal(-1)

      val (_, _) = startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, personPipelineSpec)
      waitForRowCount(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("Person"), 1)
   }


   @Test
   fun `can invoke transform function and persist query output type with custom table name`() {
      val taxiDef = """
         model Input {
            filmId : FilmId inherits String
         }

         model Film {
            title : FilmTitle inherits String
            review : ReviewScore inherits Int
         }

         service FilmService {
            operation lookupFilm(FilmId): Film
         }

         type PosterQuote inherits String
         model Output {
            score : ReviewScore
            posterQuote : PosterQuote
         }
      """.trimIndent()
      val testSetup = jetWithSpringAndVyne(taxiDef, listOf(postgresSQLContainerFacade.connection))
      val schema = testSetup.schema
      testSetup.stubService.addResponse(
         "lookupFilm",
         parseJson(schema, "Film", """{ "title" : "Star Wars" , "review" : 4.99 }""")
      )
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(
         testSetup.applicationContext,
         targetType = "Output"
      )
      val pipelineSpec = PipelineSpec(
         name = "transforming",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "filmId" : "star-1"  }"""),
            typeName = "Input".fqn()
         ),
         transformation = """find { Film } as {
            |score : ReviewScore
            |posterQuote : PosterQuote = "Triffic."
            |}""".trimMargin(),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               targetTypeName = null,
               tableName = "MovieQuotes"
            )
         )
      )
      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)
      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)
      // The table should get created with the specified name
      waitForTableExistence(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         "MovieQuotes".lowercase(),
         true
      )

      // Our transformed row should be inserted.
      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         null,
         1,
         duration = Duration.ofSeconds(30L),
         tableName = "MovieQuotes"
      )

   }

   private fun waitForRowCount(
      dsl: DSLContext,
      type: Type?,
      rowCount: Int,
      startTime: Instant = Instant.now(),
      duration: Duration = Duration.ofSeconds(10),
      tableName: String? = null
   ) {
      Awaitility.await().atMost(duration)
         .until {
            val currentRowCount = rowCount(dsl, type, tableName)
            logger.info(
               "Row count after ${
                  Duration.between(startTime, Instant.now()).toMillis()
               }ms is $currentRowCount (Waiting until it hits $rowCount)"
            )
            currentRowCount >= rowCount
         }
   }

   private fun waitForTableExistence(
      dsl: DSLContext,
      tableName: String,
      shouldExist: Boolean,
      startTime: Instant = Instant.now(),
      duration: Duration = Duration.ofSeconds(10)
   ) {
      Awaitility.await().atMost(duration)
         .until {
            val tableExists = dsl.meta().tables.any { it.name == tableName }
            val isCorrect = tableExists == shouldExist
            logger.info(
               "The table $tableName does ${if (tableExists) "" else "not "}exist while it should ${if (shouldExist) "" else "not "}exist after ${
                  Duration.between(
                     startTime,
                     Instant.now()
                  ).toMillis()
               }ms"
            )
            isCorrect
         }
   }

   private fun rowCount(dsl: DSLContext, type: Type?, tableName: String? = null): Int {
      val table = tableName?.let { table(it) } ?: table(SqlUtils.tableNameOrTypeName(type!!.taxiType))
      return try {
         dsl.fetchCount(table)
      } catch (e: Exception) {
         -1
      } // return -1 if the table doesn't exist
   }
}
