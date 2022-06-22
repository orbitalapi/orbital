package io.vyne.spring.invokers.http

import app.cash.turbine.test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.expectTypedObjects
import io.vyne.http.MockWebServerRule
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.OperationNames
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.invokers.RestTemplateInvoker
import io.vyne.testVyne
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Sinks
import java.time.Duration

class BatchingHttpInvokerTest {

   @Rule
   @JvmField
   val mockWebServer = MockWebServerRule()

   @Test
   fun `operation is batchable if a batch lookup exists which accepts a request with a property of multiple ids`() {
      val schema = TaxiSchema.from(
         """
          type MovieId inherits Int
          model Movie {
            @Id id : MovieId
          }
          model MovieLookupRequest {
            ids : MovieId[]
          }
          service MovieService {
            operation findMultiple(MovieLookupRequest):Movie[]
            operation getMovie(MovieId):Movie
         }
      """.trimIndent()
      )
      val invoker = batchingInvoker()
      val (service, operation) = schema.operation(OperationNames.qualifiedName("MovieService", "getMovie"))
      invoker.canBatch(service, operation, schema).should.be.`true`
   }

   private fun batchingInvoker(
      restTemplateInvoker: RestTemplateInvoker = mock(),
      batchSettings: BatchSettings = BatchSettings()
   ): BatchingHttpInvoker {
      return BatchingHttpInvoker(
         restTemplateInvoker, batchSettings
      )
   }

   @Test
   fun `operation is not batchable if the input is not an id`() {
      val schema = TaxiSchema.from(
         """
          type MovieId inherits Int
          model Movie {
          // id is not an @Id - so should prevent batching
            id : MovieId
          }
          model MovieLookupRequest {
            ids : MovieId[]
          }
          service MovieService {
            operation findMultiple(MovieLookupRequest):Movie[]
            operation getMovie(MovieId):Movie
         }
      """.trimIndent()
      )
      val invoker = batchingInvoker()
      val (service, operation) = schema.operation(OperationNames.qualifiedName("MovieService", "getMovie"))
      invoker.canBatch(service, operation, schema).should.be.`false`
   }

   @Test
   fun `operation is batchable if it accepts a collection of ids`() {
      val schema = TaxiSchema.from(
         """
          type MovieId inherits Int
          model Movie {
          @Id
            id : MovieId
          }
          service MovieService {
            operation findMultiple(MovieId[]):Movie[]
            operation getMovie(MovieId):Movie
         }
      """.trimIndent()
      )
      val invoker = batchingInvoker()
      val (service, operation) = schema.operation(OperationNames.qualifiedName("MovieService", "getMovie"))
      invoker.canBatch(service, operation, schema).should.be.`true`
   }

   @Test
   fun `operation is not batchable if the collection is not of ids`() {
      val schema = TaxiSchema.from(
         """
          type MovieId inherits Int
          model Movie {
            // id is not an @Id, so not batchable
            id : MovieId
          }
          service MovieService {
            operation findMultiple(MovieId[]):Movie[]
            operation getMovie(MovieId):Movie
         }
      """.trimIndent()
      )
      val invoker = batchingInvoker()
      val (service, operation) = schema.operation(OperationNames.qualifiedName("MovieService", "getMovie"))
      invoker.canBatch(service, operation, schema).should.be.`false`
   }


   @Test
   fun `batching http request integration test`(): Unit = runBlocking {
      // SETUP
      val schema = TaxiSchema.from(
         """
          type MovieId inherits Int
          model Movie {
            @Id
            id : MovieId
            title : MovieTitle inherits String
          }
          model NewRelease {
            @Id id : MovieId
          }
          service NewReleases {
            operation listNewReleases():Stream<NewRelease>
          }

          service MovieService {
            @HttpOperation(method = 'POST', url = "${mockWebServer.url("/findMultiple")}")
            operation findMultiple(@RequestBody MovieId[]):Movie[]
            @HttpOperation(method = 'GET', url = "${mockWebServer.url("/getOne")}")
            operation getMovie(MovieId):Movie
         }
      """.trimIndent()
      )
      val stub = StubService(schema = schema)

      mockWebServer.addJsonResponse(
         """[ { "id" : 1 , "title" : "movie-1" },
         | { "id" : 2 , "title" : "movie-2" },
         | { "id" : 3 , "title" : "movie-3" }
         | ]
      """.trimMargin()
      )

      val restTemplateInvoker = restTemplateInvoker(schema)
      val batchingHttpStrategy =
         batchingInvoker(restTemplateInvoker, BatchSettings(batchSize = 3, batchTimeout = Duration.ofMinutes(1)))
      val vyne = testVyne(
         schema,
         listOf(restTemplateInvoker, stub),
         listOf(batchingHttpStrategy)
      )
      val newReleasesSink = Sinks.many().unicast().onBackpressureBuffer<TypedInstance>()
      stub.addResponseFlow("listNewReleases", newReleasesSink.asFlux().asFlow())

      fun emitNewRelease(id: Int) {
         newReleasesSink.tryEmitNext(vyne.parseJson("NewRelease", """{   "id" : $id }"""))
      }


      // Test -- run a streaming query.  Each
      // result needs to be enriched against an HTTP service
      vyne.query(
         """stream { NewRelease } as {
         | id: MovieId
         | title : MovieTitle }[]""".trimMargin()
      )
         .results
         .test {

            // Assert...
            emitNewRelease(1)
            emitNewRelease(2)
            emitNewRelease(3)

            val typedObjects = expectTypedObjects(3)
               .map { it.toRawObject()!! }
            typedObjects.should.have.size(3)
            typedObjects.should.contain(mapOf("id" to 1, "title" to "movie-1"))
            typedObjects.should.contain(mapOf("id" to 2, "title" to "movie-2"))
            typedObjects.should.contain(mapOf("id" to 3, "title" to "movie-3"))

            newReleasesSink.tryEmitComplete()

            awaitComplete()

            // assert the batch operation was called, with a batch of ids
            mockWebServer.get().requestCount.should.equal(1)
            val request = mockWebServer.get().takeRequest()

            request.path.should.equal("/findMultiple")
            val requestedIds = jacksonObjectMapper().readValue<List<Int>>(request.body.inputStream())
            requestedIds.should.have.elements(1, 2, 3)
         }
   }


   fun restTemplateInvoker(schema: TaxiSchema, webClient: WebClient = WebClient.create()): RestTemplateInvoker {
      return RestTemplateInvoker(
         SimpleSchemaStore().setSchemaSet(SchemaSet.Companion.from(schema, 0)),
         webClient
      )
   }
}

