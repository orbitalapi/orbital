package com.orbitalhq.queryService.history.regressionPack

import app.cash.turbine.testIn
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.orbitalhq.cockpit.core.ConfigService
import com.orbitalhq.cockpit.core.connectors.hazelcast.HazelcastHealthCheckProvider
import com.orbitalhq.cockpit.core.content.DefaultContentRepository
import com.orbitalhq.copilot.CopilotConversationApi
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.api.RegressionPackFormat
import com.orbitalhq.history.api.RegressionPackRequest
import com.orbitalhq.history.db.LineageRecordRepository
import com.orbitalhq.history.db.PersistingTraceEventConsumer
import com.orbitalhq.history.db.QueryErrorEventRowRepository
import com.orbitalhq.history.db.QueryHistoryDbWriter
import com.orbitalhq.history.db.QueryHistoryRecordRepository
import com.orbitalhq.history.db.QueryResultRowRepository
import com.orbitalhq.history.db.QuerySankeyChartRowRepository
import com.orbitalhq.history.db.RemoteCallResponseRepository
import com.orbitalhq.history.db.tracing.TraceEventRepository
import com.orbitalhq.history.rest.QueryHistoryService
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.http.response
import com.orbitalhq.licensing.OrbitalLicenseManager
import com.orbitalhq.query.HistoryEventConsumerProvider
import com.orbitalhq.query.runtime.StreamResultStreamProvider
import com.orbitalhq.query.runtime.core.monitor.ActiveQueryController
import com.orbitalhq.queryService.BaseQueryServiceTest
import com.orbitalhq.queryService.TestSpringConfig
import com.orbitalhq.schemaServer.core.editor.SchemaEditorService
import com.orbitalhq.schemaServer.core.packages.PackageService
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectSpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.spring.invokers.Invoker
import com.orbitalhq.spring.invokers.testVyne
import com.orbitalhq.utils.Ids
import com.winterbe.expekt.should
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.http4k.server.Http4kServer
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Import(TestSpringConfig::class, PersistingTraceEventConsumer::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=true",
      "vyne.analytics.persistRemoteCallResponses=true",
      "vyne.analytics.persistRemoteCallMetadata=true",
      "vyne.analytics.persist-trace-events=true",
      "vyne.telemetry.enabled=false",
      "vyne.analytics.writerMaxBatchSize=1",
      "vyne.analytics.writerMaxDuration=100ms",
   ]
)
class QueryAsRegressionPackTest : BaseQueryServiceTest() {
   companion object {
      @Container
      @ServiceConnection
      val postgres = PostgreSQLContainer<Nothing>("postgres:11.1").let {
         it.start()
         it.waitingFor(Wait.forListeningPort())
         it
      } as PostgreSQLContainer<*>

   }

   @Autowired
   lateinit var traceEventConsumer: PersistingTraceEventConsumer

   @MockitoBean
   lateinit var streamResultStreamProvider: StreamResultStreamProvider

   @MockitoBean
   lateinit var chatService: CopilotConversationApi

   @MockitoBean
   lateinit var cmsService: DefaultContentRepository

   @MockitoBean
   lateinit var eventDispatcher: ProjectSpecLifecycleEventDispatcher

   @MockitoBean
   lateinit var reactiveProjectStoreManager: ReactiveProjectStoreManager

   @MockitoBean
   lateinit var configLoader: WorkspaceConfigLoader

   @MockitoBean
   lateinit var hazelcastHealthCheckProvider: HazelcastHealthCheckProvider

   @MockitoBean
   lateinit var configService: ConfigService

   @MockitoBean
   lateinit var licenseManager: OrbitalLicenseManager

   @Autowired
   lateinit var traceEventRepository: TraceEventRepository

   @Autowired
   lateinit var queryHistoryRecordRepository: QueryHistoryRecordRepository

   @Autowired
   lateinit var resultRowRepository: QueryResultRowRepository

   @Autowired
   lateinit var lineageRepository: LineageRecordRepository

   @Autowired
   lateinit var historyDbWriter: QueryHistoryDbWriter

   @Autowired
   lateinit var historyService: QueryHistoryService

   @Autowired
   lateinit var activeQueryController: ActiveQueryController

   @MockitoBean
   lateinit var packagesService: PackageService

   @MockitoBean
   lateinit var schemaEditorService: SchemaEditorService

   @Autowired
   lateinit var lineageRecordRepository: LineageRecordRepository


   @Autowired
   lateinit var remoteCallResponseRepository: RemoteCallResponseRepository

   @Autowired
   lateinit var errorEventRowRepository: QueryErrorEventRowRepository


   @Autowired
   lateinit var sankeyChartRowRepository: QuerySankeyChartRowRepository

   @Rule
   @JvmField
   final val tempDir = TemporaryFolder()

   @Rule
   @JvmField
   final val server = MockWebServerRule()

   @Deprecated("Move to server from MockWebServerRule, to be consistent")
   var http4kServer: Http4kServer? = null


   @After
   fun tearDown() {
      if (http4kServer != null) {
         http4kServer!!.stop()
      }
   }

   @Test
   @Ignore // I can't get this to persist trace events, and I can't work out why
   fun `can export a query with http calls as a preflight test spec`() {
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
      var queryId:String? = null
      runBlocking {
         val result = queryService.submitVyneQlQueryStreamingResponse(
            """
         find { Movie[] } as {
            id : MovieId
            title : MovieTitle
            cast : Cast[]
         }[]
      """.trimIndent(), clientQueryId = clientQueryId
         ).toList()
         result.shouldHaveSize(1)
      }

      Awaitility.await()
         .atMost(Duration.FIVE_SECONDS)
         .until<Boolean> {
            queryHistoryRecordRepository.findByClientQueryId(clientQueryId)
               ?.let { historyRecord ->
                  queryId = historyRecord.queryId
                  true
               }
         }
      val calls = remoteCallResponseRepository.findAllByQueryId(queryId!!)
      calls.shouldHaveSize(2)
      calls.forEach { it.response.shouldNotBeNull() }

      val byteBuffer = historyService.getRegressionPack(queryId!!, RegressionPackRequest(
         queryId,
         "A test with http calls",
         RegressionPackFormat.Preflight,
         "This is a test that should show http calls"
      )).block()!!
      byteBuffer
   }

   private fun buildHistoryConsumer(): HistoryEventConsumerProvider {
      return QueryHistoryDbWriter(
         queryHistoryRecordRepository,
         resultRowRepository,
         lineageRecordRepository,
         remoteCallResponseRepository,
         sankeyChartRowRepository,
         errorEventRowRepository = errorEventRowRepository,
         config = QueryAnalyticsConfig(
            persistenceQueueStorePath = tempDir.root.toPath()
         ),
      )
   }
}

