package io.vyne.queryService.history.db

import app.cash.turbine.test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.http.respondWith
import io.vyne.http.response
import io.vyne.models.FailedSearch
import io.vyne.models.OperationResult
import io.vyne.models.TypedNull
import io.vyne.query.QueryResponse
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseCodeGroup
import io.vyne.query.ResponseMessageType
import io.vyne.query.ResultMode
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.history.QueryHistoryService
import io.vyne.queryService.query.FirstEntryMetadataResultSerializer.ValueWithTypeName
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.fqn
import io.vyne.spring.invokers.Invoker
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.testVyne
import io.vyne.typedObjects
import io.vyne.utils.Benchmark
import io.vyne.utils.StrategyPerformanceProfiler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private val logger = KotlinLogging.logger {}

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@SpringBootTest
class QueryHistoryPersistenceTest : BaseQueryServiceTest() {

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

   @Rule
   @JvmField
   final val server = MockWebServerRule()

   @Deprecated("Move to server from MockWebServerRule, to be consistent")
   var http4kServer: Http4kServer? = null

   @Before
   fun setup() {
      historyDbWriter.persistenceBufferDuration = Duration.ofMillis(5)
      historyDbWriter.persistenceBufferSize = 2
   }

   @After
   fun tearDown() {
      if (http4kServer != null) {
         http4kServer!!.stop()
      }
   }

   @Test
   fun `can persist resultRow to jpa`() {
      val persisted = resultRowRepository.save(
         QueryResultRow(
            queryId = "queryId",
            json = "{ foo : Bar }",
            valueHash = 123
         )
      )
      persisted.rowId.should.not.be.`null`
   }

   @Test
   @Transactional
   fun `can persist querySummary to jpa`() {
      // Create a string 100,000 1's numbers long (ie., 100k characters)
      val largeString = (0 until 100000).joinToString(separator = "") { "1" }
      val querySummary = queryHistoryRecordRepository.save(
         QuerySummary(
            queryId = "queryId",
            clientQueryId = "clientQueryId",
            taxiQl = "findAll { Foo[] }",
            queryJson = largeString,
            startTime = Instant.now(),
            responseStatus = QueryResponse.ResponseStatus.RUNNING,
            anonymousTypesJson = largeString
         )
      )
      querySummary.id.should.not.be.`null`
      queryHistoryRecordRepository.findByClientQueryId("clientQueryId").id.should.equal(querySummary.id)
      queryHistoryRecordRepository.findByQueryId("queryId").id.should.equal(querySummary.id)

      val endTime = Instant.now().plusSeconds(10)
      val updatedCount = queryHistoryRecordRepository.setQueryEnded(
         queryId = "queryId",
         endTime = endTime,
         status = QueryResponse.ResponseStatus.COMPLETED,
         message = "All okey dokey"
      )
      updatedCount.should.equal(1)

      val updated = queryHistoryRecordRepository.findByQueryId(querySummary.queryId)
      updated.endTime.should.equal(endTime)
      updated.responseStatus.should.equal(QueryResponse.ResponseStatus.COMPLETED)
      updated.errorMessage.should.equal("All okey dokey")
   }


   @Test
   @Ignore // FLakey
   fun `can read and write query results to db from restful query`() {
      setupTestService(historyDbWriter)
      val query = buildQuery("Order[]")
      val id = query.queryId

      runBlocking {
         val response = queryService.submitQuery(query, ResultMode.SIMPLE, MediaType.APPLICATION_JSON_VALUE)
            .body!!
            .test {
               val first = expectItem()
               expectComplete()
            }
      }

      Thread.sleep(2000)
      val results = resultRowRepository.findAllByQueryId(id)

      results.should.have.size(1)

      val queryHistory = queryHistoryRecordRepository.findByQueryId(id)

      queryHistoryRecordRepository.findById(queryHistory.id!!)
         .let { updatedHistoryRecord ->
            updatedHistoryRecord
            // Why sn't this workig?
            updatedHistoryRecord.get().endTime.should.not.be.`null`
         }
   }

   @Test
   @Ignore // flakey
   fun `can read and write query results from taxiQl query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runBlocking {
         queryService.submitVyneQlQuery("findAll { Order[] } as Report[]", clientQueryId = id)
            .body
            .test(timeout = 10.seconds) {
               val first = expectItem()
               first.should.not.be.`null`
               expectComplete()
            }
      }

      await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord != null && historyRecord.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord.taxiQl.should.equal("findAll { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.have.size(1)

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.remoteCalls.should.have.size(3)
   }

   @Test
   fun `failed http calls are present in history`() {
      val randomPort = Random.nextInt(10000, 12000)
      val vyne = testVyne(
         """
         model Book {
            title : BookTitle inherits String
            authorId : AuthorId inherits Int
         }
         model Author {
            @Id
            id : AuthorId
            name : AuthorName inherits String
         }
         model Output {
            title : BookTitle
            authorName : AuthorName
         }
         service Service {
            @HttpOperation(method = "GET" , url = "http://localhost:$randomPort/books")
            operation listBooks():Book[]
            @HttpOperation(method = "GET" , url = "http://localhost:$randomPort/authors/{authorId}")
            operation findAuthor(@PathVariable("authorId") authorId: AuthorId):Author
         }
      """.trimIndent()
      ) { schema ->
         listOf(
            CacheAwareOperationInvocationDecorator(
               RestTemplateInvoker(
                  SimpleSchemaProvider(schema),
                  WebClient.builder(),
                  ServiceUrlResolver.DEFAULT
               )
            )
         )
      }

      http4kServer = routes(
         "/books" bind GET to { Response(OK).body(""" [ { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 } ]""") },
         "/authors/2" bind GET to { Response(NOT_FOUND).body("""No author with id 2 found""") }
      ).asServer(Netty(randomPort)).start()

      setupTestService(vyne, null, historyDbWriter)

      val id = UUID.randomUUID().toString()

      val query = "findAll { Book[] } as Output[]"
      var firstResult: ValueWithTypeName? = null
      runBlocking {
         queryService.submitVyneQlQuery(query, clientQueryId = id, resultMode = ResultMode.SIMPLE)
            .body
            .test(kotlin.time.Duration.INFINITE) {
               val first = expectItem()
               firstResult = first as ValueWithTypeName
               first.should.not.be.`null`
               expectItem()
               expectItem()
               expectComplete()
            }
      }

      // Check the lineage on the results
      runBlocking {
         val output = vyne.query(query).typedObjects().first()
         val authorName = output["authorName"]
         authorName.should.be.instanceof(TypedNull::class.java)
         authorName.source.should.be.instanceof(FailedSearch::class.java)
         val source = authorName.source as FailedSearch
         source.failedAttempts.should.have.size(1)
         val failedCallSource = source.failedAttempts.first() as OperationResult
         failedCallSource.remoteCall.resultCode.should.equal(404)
      }

      Thread.sleep(2000) // Allow persistence to catch up

      val profileData = historyService.getQueryProfileDataFromClientId(id)
      val remoteCalls = profileData.remoteCalls
      remoteCalls.should.have.size(2)

      // We should see a failure in the operation stats
      val findAuthorStats = profileData.operationStats.first { it.operationName == "findAuthor" }
      findAuthorStats.responseCodes.should.contain(ResponseCodeGroup.HTTP_4XX to 1)
      findAuthorStats.callsInitiated.should.equal(1)

      // Should have rich lineage around the null value
      val nodeDetail = historyService.getNodeDetail(firstResult?.queryId!!, firstResult!!.valueId, "authorName")
      // FIXME same as above
//      nodeDetail.source.should.not.be.empty
   }

   @Test
   fun `remote calls leading to duplicate lineage results are persisted without exceptions`() {
      val randomPort = Random.nextInt(10000, 12000)
      val vyne = testVyne(
         """
         model Book {
            title : BookTitle inherits String
            authorId : AuthorId inherits Int
         }
         model Author {
            @Id
            id : AuthorId
            name : AuthorName inherits String
         }
         model Output {
            title : BookTitle
            authorName : AuthorName
         }
         service Service {
            @HttpOperation(method = "GET" , url = "http://localhost:$randomPort/books")
            operation listBooks():Book[]
            @HttpOperation(method = "GET" , url = "http://localhost:$randomPort/authors/{authorId}")
            operation findAuthor(@PathVariable("authorId") authorId: AuthorId):Author
         }
      """.trimIndent()
      ) { schema ->
         listOf(
            CacheAwareOperationInvocationDecorator(
               RestTemplateInvoker(
                  SimpleSchemaProvider(schema),
                  WebClient.builder(),
                  ServiceUrlResolver.DEFAULT
               )
            )
         )
      }

      http4kServer = routes(
         "/books" bind GET to { Response(OK).body(""" [ { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 } ]""") },
         "/authors/2" bind GET to { Response(OK).body("""{ "id" : 2 , "name" : "Jimmy" }""") }
      ).asServer(Netty(randomPort)).start()

      setupTestService(vyne, null, historyDbWriter)

      val id = UUID.randomUUID().toString()

      val query = "findAll { Book[] } as Output[]"
      var results = mutableListOf<ValueWithTypeName>()
      runBlocking {
         queryService.submitVyneQlQuery(query, clientQueryId = id, resultMode = ResultMode.SIMPLE)
            .body
            .test(kotlin.time.Duration.INFINITE) {
               // Capture 3 results.
               results.add(expectItem() as ValueWithTypeName)
               results.add(expectItem() as ValueWithTypeName)
               results.add(expectItem() as ValueWithTypeName)
               expectComplete()
            }
      }

      // Check the lineage on the results
      runBlocking {
         val output = vyne.query(query).typedObjects().first()
         val authorName = output["authorName"]
         authorName.value!!.should.equal("Jimmy")
         authorName.source.should.be.instanceof(OperationResult::class.java)
      }

      val callable = ConditionCallable {
         historyService.getQueryProfileDataFromClientId(id)
      }
      await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until<Boolean>(callable)

      callable.result!!.remoteCalls.size == 2
      // Should have rich lineage around the null value
      val firstRecordNodeDetail = historyService.getNodeDetail(results[0].queryId!!, results[0].valueId, "authorName")
      val secondRecordNodeDetail = historyService.getNodeDetail(results[1].queryId!!, results[1].valueId, "authorName")
      firstRecordNodeDetail.should.equal(secondRecordNodeDetail)
      firstRecordNodeDetail.source.should.not.be.empty
   }

   @Test
   fun `large result set performance test`(): Unit = runBlocking {
      val recordCount = 5000

      val vyne = io.vyne.spring.invokers.testVyne(
         """
         model Movie {
            @Id id : MovieId inherits Int
            title : MovieTitle inherits String
            director : DirectorId inherits Int
            producer : ProducerId  inherits Int
         }
         model Director {
            @Id id : DirectorId
            name : DirectorName inherits String
         }
         model ProductionCompany {
            @Id id : ProducerId
            name :  ProductionCompanyName inherits String
            country : CountryId inherits Int
         }
         model Country {
            @Id id : CountryId
            name :  CountryName inherits String
         }
         model Review {
            rating : MovieRating inherits Int
         }
         service Service {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/movies")
            operation findMovies():Movie[]

             @HttpOperation(method = "GET", url = "http://localhost:${server.port}/directors/{id}")
            operation findDirector(@PathVariable("id") id : DirectorId):Director

            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/producers/{id}")
            operation findProducer(@PathVariable("id") id : ProducerId):ProductionCompany

              @HttpOperation(method = "GET", url = "http://localhost:${server.port}/countries/{id}")
            operation findCountry(@PathVariable("id") id : CountryId):Country

             @HttpOperation(method = "GET", url = "http://localhost:${server.port}/ratings")
            operation findRating():Review
         }
      """, Invoker.RestTemplateWithCache
      )
      val jackson = jacksonObjectMapper()
      val directors = (0 until 5).map { mapOf("id" to it, "name" to "Steven ${it}berg") }
      val producers = (0 until 5).map { mapOf("id" to it, "name" to "$it Studios", "country" to it) }
      val countries = (0 until 5).map { mapOf("id" to it, "name" to "Northern $it") }

      val movies = (0 until recordCount).map {
         mapOf(
            "id" to it,
            "title" to "Rocky $it",
            "director" to directors.random()["id"],
            "producer" to producers.random()["id"]
         )
      }

      Benchmark.benchmark("Heavy load", warmup = 2, iterations = 5) {
         runBlocking {
            setupTestService(vyne, null, historyDbWriter)
            val invokedPaths = ConcurrentHashMap<String, Int>()
            server.prepareResponse(
               invokedPaths,
               "/movies" to response(jackson.writeValueAsString(movies)),
               "/directors" to respondWith { path ->
                  val directorId = path.split("/").last().toInt()
                  jackson.writeValueAsString(directors[directorId])
               },
               "/producers" to respondWith { path ->
                  val producerId = path.split("/").last().toInt()
                  jackson.writeValueAsString(producers[producerId])
               },
               "/countries" to respondWith { path ->
                  val id = path.split("/").last().toInt()
                  jackson.writeValueAsString(countries[id])
               },
               "/ratings" to response(jackson.writeValueAsString(mapOf("rating" to 5)))
            )

            val query = """findAll { Movie[] } as {
         title : MovieTitle
         director : DirectorName
         producer : ProductionCompanyName
         rating : MovieRating
         country : CountryName
         }[]
      """
            val clientQueryId = UUID.randomUUID().toString()
            val result =
               queryService.submitVyneQlQuery(query, clientQueryId = clientQueryId, resultMode = ResultMode.SIMPLE)
                  .body
                  .toList()
            result.should.have.size(recordCount)
         }

      }

      val stats = StrategyPerformanceProfiler.summarizeAndReset().sortedByCostDesc()
      logger.warn("Perf test of $recordCount completed")
      logger.warn("Stats:\n ${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(stats)}")
   }


}


fun RemoteOperation.asFakeRemoteCall(): RemoteCall {
   val (serviceName, operationName) = OperationNames.serviceAndOperation(this.qualifiedName)
   return RemoteCall(
      service = serviceName.fqn(),
      address = "http://fake",
      responseTypeName = this.returnType.qualifiedName,
      method = "GET",
      operation = operationName,
      durationMs = 12,
      timestamp = Instant.now(),
      responseMessageType = ResponseMessageType.FULL,
      resultCode = 404,
      requestBody = null,
      response = null
   )
}

