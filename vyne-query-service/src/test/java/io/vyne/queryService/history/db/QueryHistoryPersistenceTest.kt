package io.vyne.queryService.history.db

import app.cash.turbine.test
import com.jayway.awaitility.Awaitility.await
import com.winterbe.expekt.should
import io.vyne.models.FailedSearch
import io.vyne.models.OperationResult
import io.vyne.models.TypedNull
import io.vyne.query.RemoteCall
import io.vyne.query.ResponseCodeGroup
import io.vyne.query.ResponseMessageType
import io.vyne.query.ResultMode
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.queryService.BaseQueryServiceTest
import io.vyne.queryService.history.QueryHistoryService
import io.vyne.queryService.query.FirstEntryMetadataResultSerializer
import io.vyne.schemaStore.SimpleSchemaProvider
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.fqn
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.spring.invokers.ServiceUrlResolver
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@ExperimentalTime
@ExperimentalCoroutinesApi
//@ContextConfiguration(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
@SpringBootTest(
   //classes = [TestConfig::class]
)
@Ignore
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

   var http4kServer:Http4kServer? = null

   @After
   fun tearDown() {
      if (http4kServer != null) {
         http4kServer!!.stop()
      }
   }

   @Test
   @Ignore
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
         .collectList().block()
      results.should.have.size(1)

      val queryHistory = queryHistoryRecordRepository.findByQueryId(id).block()

      queryHistoryRecordRepository.findById(queryHistory.id!!).block()
         .let { updatedHistoryRecord ->
            // Why sn't this workig?
            updatedHistoryRecord.endTime.should.not.be.`null`
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
            .block()
         historyRecord != null && historyRecord.endTime != null
      }

      val historyRecord = queryHistoryRecordRepository.findByClientQueryId(id)
         .block()
      historyRecord.should.not.be.`null`
      historyRecord.taxiQl.should.equal("findAll { Order[] } as Report[]")
      historyRecord.endTime.should.not.be.`null`

      val results = resultRowRepository.findAllByQueryId(id)
         .collectList().block()
      results.should.have.size(1)

      val historyProfileData = historyService.getQueryProfileDataFromClientId(id).block()!!
      historyProfileData.remoteCalls.should.have.size(3)
   }

   @Test
   fun `failed http calls are present in history`() {
      val randomPort = Random.nextInt(10000,12000)
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
      ) { schema -> listOf(CacheAwareOperationInvocationDecorator(RestTemplateInvoker(SimpleSchemaProvider(schema), WebClient.builder(), ServiceUrlResolver.DEFAULT))) }

      http4kServer = routes(
         "/books" bind GET to { Response(OK).body(""" [ { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 }, { "title" : "How to get taller" , "authorId" : 2 } ]""")},
         "/authors/2" bind GET to { Response(NOT_FOUND).body("""No author with id 2 found""")}
      ).asServer(Netty(randomPort)).start()

      setupTestService(vyne, null, historyDbWriter)

      val id = UUID.randomUUID().toString()

      val query = "findAll { Book[] } as Output[]"
      var firstResult:FirstEntryMetadataResultSerializer.ValueWithTypeName? = null
      runBlocking {
         queryService.submitVyneQlQuery(query, clientQueryId = id, resultMode = ResultMode.SIMPLE)
            .body
            .test(kotlin.time.Duration.INFINITE) {
               val first = expectItem()
               firstResult = first as FirstEntryMetadataResultSerializer.ValueWithTypeName
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

      val profileData = historyService.getQueryProfileDataFromClientId(id).block()!!
      val remoteCalls = profileData.remoteCalls
      remoteCalls.should.have.size(2)

      // We should see a failure in the operation stats
      val findAuthorStats = profileData.operationStats.first { it.operationName == "findAuthor" }
      findAuthorStats.responseCodes.should.contain(ResponseCodeGroup.HTTP_4XX to 1)
      findAuthorStats.callsInitiated.should.equal(1)

      // Should have rich lineage around the null value
      val nodeDetail = historyService.getNodeDetail(firstResult?.queryId!!, firstResult!!.valueId, "authorName").block()
      // FIXME same as above
//      nodeDetail.source.should.not.be.empty
   }


}


fun RemoteOperation.asFakeRemoteCall():RemoteCall {
   val (serviceName,operationName) = OperationNames.serviceAndOperation(this.qualifiedName)
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
