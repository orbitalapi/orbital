package io.vyne.spring.invokers.http.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.http.MockWebServerRule
import io.vyne.models.TypedInstance
import io.vyne.rawObjects
import io.vyne.schemas.OperationNames
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Duration

class CollectionOfRelatedObjectsBatchingTest {

   @Rule
   @JvmField
   val mockWebServer = MockWebServerRule()

   fun schema(testSpecificSchema: String) = TaxiSchema.from(
      """
          type MovieId inherits Int
          model Movie {
            @Id
            id : MovieId
            title : MovieTitle inherits String
          }
          model Actor {
            @Id id : ActorId inherits Int
            name : ActorName inherits String
          }

          service NewReleases {
            operation listMovies():Movie[]
          }

$testSpecificSchema

      """.trimIndent()
   )

   @Test
   // Sorry, future me ... I hope I come up with a better name for this test.
   fun `an operation is batchable if a response exists which contains a collection of results indexed against an input param`() {
      val invoker = batchingInvoker()
      val schema = schema(
         """
           parameter model ActorLookupRequest {
             movieIds : MovieId[]
          }

          model ActorLookupResult {
             movieId: MovieId
             actors: Actor[]
          }

          service ActorService {
            // A single request
            @HttpOperation(method = 'GET', url = "${mockWebServer.url("/getOne")}")
            operation findActorsForMovie(MovieId):Actor[]

            // It's batching counterpart...
            @HttpOperation(method = 'POST', url = "${mockWebServer.url("/findMultipleActors")}")
            operation findMultipleActors(@RequestBody ActorLookupRequest):ActorLookupResult[]
         }
      """.trimIndent()
      )
      val (service, operation) = schema.operation(OperationNames.qualifiedName("ActorService", "findActorsForMovie"))
      invoker.canBatch(service, operation, schema, emptySet(), emptyList()).should.be.`true`
   }

   @Test
   fun `integration test - result returns a collection of values indexed by an input param`(): Unit = runBlocking {
      val schema = schema(
         """
           parameter model ActorLookupRequest {
             movieIds : MovieId[]
          }
          model ActorLookupResult {
             movieId: MovieId
             actors: Actor[]
          }

          service ActorService {
            // A single request
            @HttpOperation(method = 'GET', url = "${mockWebServer.url("/getOne")}")
            operation findActorsForMovie(MovieId):Actor[]

            // It's batching counterpart...
            @HttpOperation(method = 'POST', url = "${mockWebServer.url("/findMultipleActors")}")
            operation findMultipleActors(@RequestBody ActorLookupRequest):ActorLookupResult[]
         }
      """.trimIndent()
      )
      val vyne = setupTest(schema)
      mockWebServer.addJsonResponse(
         """[
            |{ "movieId" : 1 , "actors" : [{ "id" : 1, "name" : "Mark Hamill" }, {"id" : 2, "name" : "Harrison Ford" }] },
            |{ "movieId" : 2 , "actors" : [{ "id" : 2, "name" : "Harrison Ford" }, {"id" : 3, "name" : "A really big boulder" }] }
            | ]
      """.trimMargin()
      )
      val result = vyne.query(
         """find { Movie[] } as {
         | id: MovieId
         | title: MovieTitle
         | cast : Actor[]
         |}[]
      """.trimMargin()
      )
         .rawObjects()
      assertTestResult(result)
   }


   @Test
   fun `operation is batchable if result returns a top level object containing an array of values indexed by an input param`() {
      val schema = schema(
         """
           parameter model ActorLookupRequest {
             movieIds : MovieId[]
          }

          // Result is a top-level object
          model ActorLookupResult {
            // which contains a collection of the data we need, indexed.
           results: ActorLookupResultMember[]
          }
          model ActorLookupResultMember {
             movieId: MovieId
             actors: Actor[]
          }

          service ActorService {
            // A single request
            @HttpOperation(method = 'GET', url = "${mockWebServer.url("/getOne")}")
            operation findActorsForMovie(MovieId):Actor[]

            // It's batching counterpart...
            @HttpOperation(method = 'POST', url = "${mockWebServer.url("/findMultipleActors")}")
            operation findMultipleActors(@RequestBody ActorLookupRequest):ActorLookupResult
         }
      """.trimIndent()

      )
      val invoker = batchingInvoker()
      val (service, operation) = schema.operation(OperationNames.qualifiedName("ActorService", "findActorsForMovie"))
      invoker.canBatch(service, operation, schema, emptySet(), emptyList()).should.be.`true`

   }

   @Test
   fun `integration test - result returns a top level object containing an array of values indexed by an input param`() =
      runBlocking {
         val schema = schema(
            """
           parameter model ActorLookupRequest {
             movieIds : MovieId[]
          }

          // Result is a top-level object
          model ActorLookupResult {
            // which contains a collection of the data we need, indexed.
           results: ActorLookupResultMember[]
          }
          model ActorLookupResultMember {
             movieId: MovieId
             actors: Actor[]
          }

          service ActorService {
            // A single request
            @HttpOperation(method = 'GET', url = "${mockWebServer.url("/getOne")}")
            operation findActorsForMovie(MovieId):Actor[]

            // It's batching counterpart...
            @HttpOperation(method = 'POST', url = "${mockWebServer.url("/findMultipleActors")}")
            operation findMultipleActors(@RequestBody ActorLookupRequest):ActorLookupResult
         }
      """.trimIndent()
         )
         val vyne = setupTest(schema)
         mockWebServer.addJsonResponse(
            """{
            | "results" : [
            |{ "movieId" : 1 , "actors" : [{ "id" : 1, "name" : "Mark Hamill" }, {"id" : 2, "name" : "Harrison Ford" }] },
            |{ "movieId" : 2 , "actors" : [{ "id" : 2, "name" : "Harrison Ford" }, {"id" : 3, "name" : "A really big boulder" }] }
            | ]
            |}
      """.trimMargin()
         )
         val result = vyne.query(
            """find { Movie[] } as {
         | id: MovieId
         | title: MovieTitle
         | cast : Actor[]
         |}[]
      """.trimMargin()
         )
            .rawObjects()
         assertTestResult(result)
      }

   private fun assertTestResult(result: List<Map<String, Any?>>) {
      result.should.have.size(2)
      val expectedJson = """[ {
  "id" : 1,
  "title" : "Star wars - a new hope",
  "cast" : [ {
    "id" : 1,
    "name" : "Mark Hamill"
  }, {
    "id" : 2,
    "name" : "Harrison Ford"
  } ]
}, {
  "id" : 2,
  "title" : "Indiana Jones",
  "cast" : [ {
    "id" : 2,
    "name" : "Harrison Ford"
  }, {
    "id" : 3,
    "name" : "A really big boulder"
  } ]
} ]"""
      val actualJson = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
      JSONAssert.assertEquals(expectedJson, actualJson, true)


      // We should've made a single call to the batching service
      val webServer = mockWebServer.get()
      webServer.requestCount.should.equal(1)
      val request = webServer.takeRequest()
      request.path.should.equal("/findMultipleActors")
   }

   private fun setupTest(schema: TaxiSchema): Vyne {
      val stub = StubService(schema = schema)
      stub.addResponse(
         "listMovies",
         TypedInstance.from(
            schema.type("Movie[]"),
            """[ { "id" : 1, "title" : "Star wars - a new hope"}, {"id" : 2, "title": "Indiana Jones"} ]""",
            schema
         )
      )
      val restTemplateInvoker = restTemplateInvoker(schema)
      val batchingHttpStrategy =
         batchingInvoker(restTemplateInvoker, BatchSettings(batchSize = 2, batchTimeout = Duration.ofMinutes(1)))
      val vyne = testVyne(
         schema,
         listOf(restTemplateInvoker, stub),
         listOf(batchingHttpStrategy)
      )

      return vyne
   }

}
