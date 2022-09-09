package io.vyne.queryService.history.db

import app.cash.turbine.test
import app.cash.turbine.testIn
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.asPackage
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryDbWriter
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.rest.QueryHistoryService
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
import io.vyne.query.ValueWithTypeName
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.TestSpringConfig
import io.vyne.queryService.active.ActiveQueryController
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.fqn
import io.vyne.spring.invokers.Invoker
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.testVyne
import io.vyne.toParsedPackages
import io.vyne.typedObjects
import io.vyne.utils.Benchmark
import io.vyne.utils.StrategyPerformanceProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

@ExperimentalTime
@ExperimentalCoroutinesApi
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@Import(TestSpringConfig::class)
@SpringBootTest(
   properties = [
      "vyne.schema.publicationMethod=LOCAL",
      "vyne.search.directory=./search/\${random.int}",
      "vyne.analytics.persistResults=true",
      "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;MODE=LEGACY"]
)
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

   @Autowired
   lateinit var activeQueryController: ActiveQueryController

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
      queryHistoryRecordRepository.findByClientQueryId("clientQueryId")!!.id.should.equal(querySummary.id)
      queryHistoryRecordRepository.findByQueryId("queryId").id.should.equal(querySummary.id)

      val endTime = Instant.now().plusSeconds(10)
      val updatedCount = queryHistoryRecordRepository.setQueryEnded(
         queryId = "queryId",
         endTime = endTime,
         status = QueryResponse.ResponseStatus.COMPLETED,
         message = "All okey dokey",
         recordCount = 1
      )
      updatedCount.should.equal(1)

      val updated = queryHistoryRecordRepository.findByQueryId(querySummary.queryId)
      updated.endTime.should.equal(endTime)
      updated.responseStatus.should.equal(QueryResponse.ResponseStatus.COMPLETED)
      updated.errorMessage.should.equal("All okey dokey")
   }


   @Test
   //@Ignore // FLakey
   fun `can read and write query results to db from restful query`() {
      setupTestService(historyDbWriter)
      val query = buildQuery("Order[]")
      val id = query.queryId

      runBlocking {
         val response = queryService.submitQuery(query, ResultMode.TYPED, MediaType.APPLICATION_JSON_VALUE)
            .body!!
            .test {
               awaitItem()
               awaitComplete()
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
   fun `can read and write query results from taxiQl query`() {
      setupTestService(historyDbWriter)
      val id = UUID.randomUUID().toString()

      runTest {
         val turbine =
            queryService.submitVyneQlQuery("findAll { Order[] } as Report[]", clientQueryId = id).body.testIn(this)

         val first = turbine.awaitItem()
         first.should.not.be.`null`
         turbine.awaitComplete()
      }

      await().atMost(com.jayway.awaitility.Duration.TEN_SECONDS).until {
         val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         historyRecord != null && historyRecord.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)

      historyRecord.should.not.be.`null`
      historyRecord!!.taxiQl.should.equal("findAll { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`

      val results = resultRowRepository.findAllByQueryId(id)

      results.should.have.size(1)

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id)
      historyProfileData.block().remoteCalls.should.have.size(4)
   }

   @Test
   @Ignore
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
                  SimpleSchemaStore().setSchemaSet(SchemaSet.from(schema.sources, 1)),
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
      runTest {

         val turbine =
            queryService.submitVyneQlQuery(query, clientQueryId = id, resultMode = ResultMode.TYPED).body.testIn(this)
         val first = turbine.awaitItem()
         firstResult = first as ValueWithTypeName
         first.should.not.be.`null`
         turbine.awaitItem()
         turbine.awaitItem()
         turbine.awaitComplete()
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
      val remoteCalls = profileData.block().remoteCalls
      remoteCalls.should.have.size(2)

      // We should see a failure in the operation stats
      val findAuthorStats = profileData.block().operationStats.first { it.operationName == "findAuthor" }
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
                  SimpleSchemaStore().setSchemaSet(SchemaSet.fromParsed(schema.sources.asPackage().toParsedPackages(), 1)),
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
      runTest {

         val turbine =
            queryService.submitVyneQlQuery(query, clientQueryId = id, resultMode = ResultMode.TYPED).body.testIn(this)

         // Capture 3 results.
         results.add(turbine.awaitItem() as ValueWithTypeName)
         results.add(turbine.awaitItem() as ValueWithTypeName)
         results.add(turbine.awaitItem() as ValueWithTypeName)
         turbine.awaitComplete()
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

      callable.result!!.block().remoteCalls.size == 2
      // Should have rich lineage around the null value
      val firstRecordNodeDetail =
         historyService.getNodeDetail(results[0].queryId!!, results[0].valueId, "authorName").block()
      val secondRecordNodeDetail =
         historyService.getNodeDetail(results[1].queryId!!, results[1].valueId, "authorName").block()
      firstRecordNodeDetail.should.equal(secondRecordNodeDetail)
      firstRecordNodeDetail.source.should.not.be.empty
   }

   @Test
   fun `large result set performance test`(): Unit = runBlocking {
      // Set this locally something larger than 5000
      val recordCount = 5

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
               queryService.submitVyneQlQuery(query, clientQueryId = clientQueryId, resultMode = ResultMode.TYPED)
                  .body
                  .toList()
            result.should.have.size(recordCount)
         }

      }

      val stats = StrategyPerformanceProfiler.summarizeAndReset().sortedByCostDesc()
      logger.warn("Perf test of $recordCount completed")
      logger.warn("Stats:\n ${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(stats)}")
   }

   @Test
   fun `large result set performance test with cancellation`(): Unit = runBlocking {
      // Set this locally something larger than 5000
      val recordCount = 50

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
      var result = mutableListOf<ValueWithTypeName>()

      runBlocking(Job()) {
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
         var cancelSent = false
         val clientQueryId = UUID.randomUUID().toString()

         launch(Dispatchers.Default) {
            queryService.submitVyneQlQuery(query, clientQueryId = clientQueryId, resultMode = ResultMode.TYPED)
               .body
               .onEach {
                  result.add(it as ValueWithTypeName)
                  if (!cancelSent) {
                     launch {
                        queryService.activeQueryMonitor.cancelQueryByClientQueryId(clientQueryId)
                     }

                     cancelSent = true
                  }
               }
               .toList()
         }
      }



      logger.warn { "RESULT SIZE ${result.size}" }
      result.size.should.above(0)
      result.size.should.below(recordCount)
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

