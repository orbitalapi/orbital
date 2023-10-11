package com.orbitalhq.queryService.history.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Duration
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.db.*
import com.orbitalhq.history.rest.QueryHistoryService
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.http.response
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.queryService.BaseQueryServiceTest
import com.orbitalhq.queryService.TestSpringConfig
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import com.orbitalhq.spring.invokers.Invoker
import com.orbitalhq.spring.invokers.testVyne
import com.orbitalhq.utils.Ids
import io.kotest.common.runBlocking
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.flow.toList
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.ConcurrentHashMap


@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistRemoteCallMetadata=true",
      "vyne.analytics.persistRemoteCallResponses=false",
      "vyne.telemetry.enabled=false"
   ]
)
@Testcontainers
class RemoteCallMetadataPersistenceTest : BaseQueryServiceTest() {

   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

   }
   @MockBean
   lateinit var eventDispatcher: RepositorySpecLifecycleEventDispatcher

   @MockBean
   lateinit var configLoader : SchemaRepositoryConfigLoader



   @Rule
   @JvmField
   final val tempDir = TemporaryFolder()

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var lineageRecordRepository: LineageRecordRepository

   @Autowired
   lateinit var remoteCallResponseRepository: RemoteCallResponseRepository

   @Autowired
   lateinit var sankeyChartRowRepository: QuerySankeyChartRowRepository


   @Autowired
   lateinit var historyService: QueryHistoryService

   @Rule
   @JvmField
   final val server = MockWebServerRule()

   @Test
   fun `http calls made in query are persisted`() {
      val vyne = testVyne(
         """
         model Movie {
            id : MovieId inherits Int
            title : MovieTitle inherits String
         }
         model Cast {
            id : PersonId inherits String
            name : PersonName inherits String
         }
         service Movies {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/movies")
            operation listMovies():Movie[]
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/cast")
            operation getCast(@PathVariable("id") id: MovieId):Cast[]
         }
      """, Invoker.RestTemplateWithCache
      )
      setupTestService(vyne, null, buildHistoryConsumer())
      val jackson = jacksonObjectMapper()
      server.prepareResponse(
         ConcurrentHashMap(),
         "/movies" to response(
            jackson.writeValueAsString(
               listOf(mapOf("id" to 1, "title" to "Star Wars"))
            )
         ),
         "/cast" to response(
            jackson.writeValueAsString(
               listOf(mapOf("id" to "1", "name" to "Harrison Ford"))
            )
         )
      )

      val clientQueryId = Ids.id("query")
      runBlocking {
         val result = queryService.submitVyneQlQuery(
            """
         find { Movie[] } as {
            id : MovieId
            title : MovieTitle
            cast : Cast[]
         }[]
      """.trimIndent(), clientQueryId = clientQueryId
         ).body.toList()
         result.shouldHaveSize(1)
      }

      Awaitility.await()
         .atMost(Duration.FIVE_SECONDS)
         .until<Boolean> {
            // note: clientQueryId = queryId in this case.
            remoteCallResponseRepository.findAllByQueryId(clientQueryId)
               .isNotEmpty()
         }

      val calls = remoteCallResponseRepository.findAllByQueryId(clientQueryId)
      calls.shouldHaveSize(2)
      calls.forEach { it.response.shouldNotBeNull() }
   }


   @Test
   fun `failed parameterized http calls made in query are persisted`() {
      val vyne = testVyne(
         """
         model Movie {
            id : MovieId inherits Int
            title : MovieTitle inherits String
         }
         model Cast {
            id : PersonId inherits String
            name : PersonName inherits String
         }
         service Movies {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/movies")
            operation listMovies():Movie[]
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/cast")
            operation getCast(@PathVariable("id") id: MovieId):Cast[]
         }
      """, Invoker.RestTemplateWithCache
      )
      setupTestService(vyne, null, buildHistoryConsumer())
      val jackson = jacksonObjectMapper()
      server.prepareResponse(
         ConcurrentHashMap(),
         "/movies" to response(
            jackson.writeValueAsString(
               listOf(mapOf("id" to 1, "title" to "Star Wars"))
            )
         ),
         "/cast" to response("Bad request", 400),
      )

      val clientQueryId = Ids.id("query")
      runBlocking {
         try {
            val result = queryService.submitVyneQlQuery(
               """
         find { Movie[] } as {
            id : MovieId
            title : MovieTitle
            cast : Cast[]
         }[]
      """.trimIndent(), clientQueryId = clientQueryId
            ).body.toList()
            result.shouldHaveSize(1)
         } catch (e: Exception) {
         }
      }

      Awaitility.await().atMost(Duration.FIVE_SECONDS).until<Boolean> {
         historyService.getRemoteCallListByClientId(clientQueryId)
            .block().size == 2
      }
      val calls = historyService.getRemoteCallListByClientId(clientQueryId)
         .block()
      calls.shouldHaveSize(2)
      val failedCall = calls.first { it.success == false }

      val exchange = failedCall.exchange as HttpExchange
      exchange.responseCode.shouldBe(400)
   }

   @Test
   fun `failed direct http calls made in query are persisted`() {
      val vyne = testVyne(
         """
         model Movie {
            id : MovieId inherits Int
            title : MovieTitle inherits String
         }
         model Cast {
            id : PersonId inherits String
            name : PersonName inherits String
         }
         service Movies {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/movies")
            operation listMovies():Movie[]
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/cast")
            operation getCast(@PathVariable("id") id: MovieId):Cast[]
         }
      """, Invoker.RestTemplateWithCache
      )
      setupTestService(vyne, null, buildHistoryConsumer())
      val jackson = jacksonObjectMapper()
      server.prepareResponse(
         ConcurrentHashMap(),
         "/movies" to response("Bad request", 400),
      )

      val clientQueryId = Ids.id("query")
      runBlocking {
         try {
            val result = queryService.submitVyneQlQuery(
               """
         find { Movie[] } as {
            id : MovieId
            title : MovieTitle
            cast : Cast[]
         }[]
      """.trimIndent(), clientQueryId = clientQueryId
            ).body.toList()
         } catch (e: Exception) {
         }
      }
      Awaitility.await().atMost(Duration.FIVE_SECONDS).until<Boolean> {
         historyService.getRemoteCallListByClientId(clientQueryId)
            .block()
            .isNotEmpty()
      }
      val calls = historyService.getRemoteCallListByClientId(clientQueryId)
         .block()
      calls.shouldHaveSize(1)
      calls[0].success.shouldBeFalse()
      val exchange = calls[0].exchange as HttpExchange
      exchange.responseCode.shouldBe(400)
   }

   private fun buildHistoryConsumer(): HistoryEventConsumerProvider {
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         config = QueryAnalyticsConfig(
            persistenceQueueStorePath = tempDir.root.toPath()
         ),
         meterRegistry = SimpleMeterRegistry()
      )
   }

}
