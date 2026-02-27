package com.orbitalhq.spring.invokers

import app.cash.turbine.test
import app.cash.turbine.testIn
import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.expectTypedInstance
import com.orbitalhq.expectTypedObject
import com.orbitalhq.expectTypedObjectFromEither
import com.orbitalhq.firstRawObject
import com.orbitalhq.firstTypedCollection
import com.orbitalhq.firstTypedInstace
import com.orbitalhq.firstTypedObject
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.http.emptyResponse
import com.orbitalhq.http.respondWith
import com.orbitalhq.http.response
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstance.Companion.EXPIRY_METADATA
import com.orbitalhq.models.TypedNull
import com.orbitalhq.query.HttpExchange
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCallOperationResultHandler
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationEvaluator
import com.orbitalhq.query.tracing.CollectingEventSink
import com.orbitalhq.query.tracing.ConnectionError
import com.orbitalhq.query.tracing.HttpRequest
import com.orbitalhq.query.tracing.HttpResponse
import com.orbitalhq.query.tracing.NoopTracingEventSink
import com.orbitalhq.query.tracing.OperationTraceSpan
import com.orbitalhq.query.tracing.TraceContext
import com.orbitalhq.query.tracing.TraceSpan
import com.orbitalhq.query.tracing.TracingEventExchangeMetadata
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.spring.http.auth.schemes.AuthWebClientCustomizer
import com.orbitalhq.typedInstances
import com.orbitalhq.typedObjects
import com.orbitalhq.utils.Benchmark
import com.orbitalhq.utils.StrategyPerformanceProfiler
import com.orbitalhq.withBuiltIns
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mu.KotlinLogging
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

// See also VyneQueryTest for more tests related to invoking Http services
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class RestTemplateInvokerTest {

   @Rule
   @JvmField
   val server = MockWebServerRule()

   val taxiDef = """
namespace vyne {

    type CreditCostRequest {
        deets : String
    }

    type ClientId inherits String

     type CreditCostResponse {
        stuff : String
    }

    type Pet {
      id : Int
    }

    type Owner {
      name : String
    }

    type OwnedPet {
      pet : Pet
      owner : Owner
    }

    type ClientName inherits String
    model Contact {
       name: String
       surname: String
       email: String
    }

    model Client {
       name: ClientName
       contacts: Contact[]?
    }


    service CreditCostService {
        @HttpOperation(method = "POST",url = "http://localhost:{{PORT}}/costs/{vyne.ClientId}/doCalculate")
        operation calculateCreditCosts(@RequestBody CreditCostRequest, ClientId ) : CreditCostResponse
    }

    service PetService {
      @HttpOperation(method = "GET",url = "http://localhost:{{PORT}}/pets/{petId}")
      operation getPetById( petId : Int ):Pet
    }

    service ClientDataService {
        @HttpOperation(method = "GET",url = "http://localhost:{{PORT}}/clients/{vyne.ClientName}")
        operation getContactsForClient( clientName: String ) : Client
    }
}      """

   private fun expectRequestCount(count: Int) {
      assertEquals(count, server.requestCount)
   }

   private fun expectRequest(consumer: Consumer<RecordedRequest>) {
      try {
         consumer.accept(server.takeRequest())
      } catch (ex: InterruptedException) {
         throw IllegalStateException(ex)
      }
   }

   @Test
   fun `returns null when projecting a failed mutation call fails`(): Unit = runBlocking {
      val (vyne, stub) = testVyneWithStub(
         """
closed model Actor {
   name : Name inherits String
}
closed model CastList {
   cast : Actor[]
}
service FilmApi {
   @HttpOperation(method = "POST",url = "http://localhost:${server.port}/cast")
   write operation getCast():CastList
}
            """.trimIndent(),
         Invoker.RestTemplate
      )
      val invokedPaths: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
      server.prepareResponse(
         invokedPaths,
         "/cast" to response("", 503),
      )
      val result = vyne.query(
         """call FilmApi::getCast as {
         | actors : Actor[]
         |}""".trimMargin()
      )
         .firstTypedInstace()
      result.shouldBeInstanceOf<TypedNull>()
   }

   @Test
   fun `returns null when projecting and a discovery call reutrns null`(): Unit = runBlocking {
      val (vyne, stub) = testVyneWithStub(
         """
closed model Actor {
   name : Name inherits String
}
closed model CastList {
   cast : Actor[]
}
service FilmApi {
   @HttpOperation(method = "POST",url = "http://localhost:${server.port}/cast")
   write operation getCast():CastList
}
            """.trimIndent(),
         Invoker.RestTemplate
      )
      val invokedPaths: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
      server.prepareResponse(
         invokedPaths,
         "/cast" to response("", 200),
      )
      val result = vyne.query(
         """call FilmApi::getCast as {
         | actors : Actor[]
         |}""".trimMargin()
      )
         .firstTypedInstace()
      result.shouldBeInstanceOf<TypedNull>()
   }

   @Test
   fun `will enrich a stream against a rest api when first rest call fails with 400`() = runBlocking {
      val (vyne, stub) = testVyneWithStub(
         """
         type FilmId inherits Int
         model Film {
            @Id
            filmId : FilmId
            title : FilmTitle inherits String
         }
         model NewReleaseAnnouncement {
            filmId : FilmId
         }
         service FilmService {
            @HttpOperation(method = "GET",url = "http://localhost:${server.port}/films/{filmId}")
            operation lookupFilm(@PathVariable("filmId") filmId: FilmId):Film
            operation streamAnnouncements():Stream<NewReleaseAnnouncement>
         }
      """.trimIndent(),
         Invoker.RestTemplate
      )
      stub.addResponseFlow("streamAnnouncements") { _, _ ->
         val typedInstance1 = TypedInstance.tryFrom(
            vyne.type("NewReleaseAnnouncement"),
            mapOf("filmId" to 1),
            vyne.schema
         )

         val typedInstance2 = TypedInstance.tryFrom(
            vyne.type("NewReleaseAnnouncement"),
            mapOf("filmId" to 2),
            vyne.schema
         )
         listOf(typedInstance1, typedInstance2).asFlow()
      }

      val invokedPaths: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
      server.prepareResponse(
         invokedPaths,
         "/films/1" to response("", 400),
         "/films/2" to response("""{ "filmId" : 2, "title" : "A new hope" }"""),
      )

      vyne.query(
         """stream { NewReleaseAnnouncement } as {
         | filmId : FilmId
         | title : FilmTitle
         | }[]
      """.trimMargin()
      ).results.test {
         val item1 = expectTypedObject()
         item1.toRawObject().should.equal(
            mapOf(
               "filmId" to 1,
               "title" to null
            )
         )

         val item2 = expectTypedObject()
         item2.toRawObject().should.equal(
            mapOf(
               "filmId" to 2,
               "title" to "A new hope"
            )
         )
         awaitComplete()

      }
   }

   @Test
   fun `will mutate a stream against a rest api when first rest call fails with 400`() = runBlocking {
      val (vyne, stub) = testVyneWithStub(
         """
         type FilmId inherits Int
         model Film {
            @Id
            filmId : FilmId
            title : FilmTitle inherits String
         }
          model NewReleaseAnnouncement {
            filmId : FilmId
         }

         parameter model FilmUpdate {
            filmId: FilmId
         }
         service FilmService {
            @HttpOperation(method = "POST",url = "http://localhost:${server.port}/films/{filmId}")
            write operation persistFilm(@RequestBody FilmUpdate, @PathVariable("filmId") filmId: FilmId):Film
            operation streamAnnouncements():Stream<NewReleaseAnnouncement>
         }
      """.trimIndent(),
         Invoker.RestTemplate
      )
      stub.addResponseFlow("streamAnnouncements") { _, _ ->
         val typedInstance1 = TypedInstance.tryFrom(
            vyne.type("NewReleaseAnnouncement"),
            mapOf("filmId" to 1),
            vyne.schema
         )

         val typedInstance2 = TypedInstance.tryFrom(
            vyne.type("NewReleaseAnnouncement"),
            mapOf("filmId" to 2),
            vyne.schema
         )
         listOf(typedInstance1, typedInstance2).asFlow()
      }

      val invokedPaths: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
      server.prepareResponse(
         invokedPaths,
         "/films/1" to response("", 404),
         "/films/2" to response("""{ "filmId" : 2, "title" : "A new hope" }"""),
      )

      vyne.query(
         """stream { NewReleaseAnnouncement }
                | call FilmService::persistFilm
      """.trimMargin()
      ).results.test {
         val item1 = expectTypedInstance()
         val item2 = expectTypedInstance()

         // Either item1 or item2 should be TypedNull for failed mutation
         if (item2 !is TypedNull && item1 !is TypedNull) {
            fail("mutation failure should yield an TypedNull")
         }
         awaitComplete()

      }
   }

   @Test
   @OptIn(ExperimentalTime::class)
   fun `When invoked a service that returns a list property mapped to a taxi array`() {
      val json = """
            {
               "name" : "Notional",
               "contacts":
                 [
                  {
                     "name": "Marty",
                     "surname": "Pitt",
                     "email": "marty.pitt@vyne.co"
                  },
                  {
                     "name": "John",
                     "surname": "Doe",
                     "email": "john.doe@vyne.co"
                  }
                 ],
               "clientAttributes" : [ { } ]
           }""".trimIndent()


      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json)
         response.setHeader("Cache-Control", "max-age=604800, must-revalidate")
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.ClientDataService")
      val operation = service.operation("getContactsForClient")
      val queryContext: QueryContext = eventCapturingQueryContext().first


      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         )
            .invoke(
               service, operation, listOf(
                  paramAndType("vyne.ClientName", "notional", schema)
               ), queryContext, "MOCK_QUERY_ID", QueryOptions()
            ).testIn(this)
         val instance = turbine.expectTypedObjectFromEither()
         expect(instance.type.fullyQualifiedName).to.equal("vyne.Client")
         expect(instance["name"].value).to.equal("Notional")
         expect((instance["contacts"] as TypedCollection)).size.to.equal(2)
         expect(instance.metadata.containsKey(EXPIRY_METADATA))
         turbine.awaitComplete()
         expectRequestCount(1)
         expectRequest { request ->
            request.getHeader("Accept").should.equal(RestTemplateInvoker.defaultAcceptHeaderValue.joinToString { "$it" })
            assertEquals("/clients/notional", request.path)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }
      }
   }


   @Test
   fun `invoke a restTemplate from vyne`() {
      val json = """
         [{ "firstName" : "Jimmy", "lastName" : "Pitt", "id" : "123" }]
      """.trimIndent()

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json)
         response.setHeader("Cache-Control", "max-age=604800, no-transform")
      }

      val vyne = testVyne(
         """
         type FirstName inherits String
         type LastName inherits String
         type PersonId inherits String
         model Person {
            firstName : FirstName
            lastNAme : LastName
            id : PersonId
         }

         service PersonService {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/people")
            operation `findAll`() : Person[]
         }
      """, Invoker.RestTemplateWithCache
      )



      runTest {
         val turbine = vyne.query("find { Person[] }").results.testIn(this)
         val instance = turbine.expectTypedObject()
         expect(instance.metadata.containsKey(EXPIRY_METADATA))
         turbine.awaitComplete()

         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/people", request.path)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }
      }
   }

   @Test
   fun `when service returns an http error subsequent attempts get the error replayed`(): Unit = runBlocking {
      val buildNewVyne = {
         testVyne(
            """
         model Person {
            name : Name inherits String
            country : CountryId inherits Int
         }
         model Country {
            @Id id : CountryId
            name : CountryName inherits String
         }
         service Service {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/people")
            operation findPeople():Person[]
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country?id={id}")
            operation findCountry(@RequestParam("id") id : CountryId):Country
         }
      """, Invoker.RestTemplateWithCache
         )
      }

      // Test for multiple error codes
      val invokedPaths: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
      listOf(400, 404, 500, 503).forEach { errorCode ->
         invokedPaths.clear()
         server.prepareResponse(
            invokedPaths,
            "/people" to response("""[ { "name" : "jimmy" , "country" : 1 }, {"name" : "jack", "country" : 1 }, {"name" : "jones", "country" : 1 }]"""),
            "/country?id=1" to response("", errorCode)
         )

         // Create a new vyne instance to destroy the cache between loops
         val result = buildNewVyne().query(
            """find { Person[] } as {
         personName : Name
         countryName : CountryName }[]"""
         )
            .typedObjects()

         // Should've only called once
         invokedPaths["/country?id=1"].should.equal(1)

         result.map { it["countryName"] }
            .forEach { countryName ->
               countryName.source.failedAttempts.should.have.size(1)
               countryName.source.failedAttempts.first().should.be.instanceof(OperationResultReference::class.java)

            }

      }
   }

   @Test
   fun `when there are multiple paths available and a service throws an exception in one of the paths the service is replayed from the cache on the other paths`(): Unit =
      runBlocking {
         val vyne = testVyne(
            """
         model Person {
            name : Name inherits String
            countryId : CountryId inherits String
         }
         model Country {
            name : CountryName inherits String
         }
         type CountryIsoCode inherits String
         service Service {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/people")
            operation findPeople():Person[]

            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country/{countryId}/isoCode")
            operation findCountryIsoCode(@PathVariable("countryId") countryId:CountryId):CountryIsoCode

            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country/iso/{countryIso}/name")
            operation findCountryNameFromIso(@PathVariable("countryIso") countryIso:CountryIsoCode):CountryName

            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country/{countryId}/name")
            operation findCountryName(@PathVariable("countryId") countryId: CountryId):CountryName
         }
      """, Invoker.RestTemplateWithCache
         )
         val invokedPaths = ConcurrentHashMap<String, Int>()
         server.prepareResponse(
            invokedPaths,
            "/people" to response("""[ { "name" : "jimmy" , "countryId" : "nz"  } , {"name": "jones", "countryId" : "nz" }]"""),
            "/country/nz/name" to response("Unknown country id", 404),
            "/country/nz/isoCode" to response("NZD"),
            "/country/iso/NZD/name" to response("New Zealand")
         )

         val response = vyne.query("""find { Person[] } as { name : Name country : CountryName }[]""")
            .rawObjects()
         response.should.equal(
            listOf(
               mapOf("name" to "jimmy", "country" to "New Zealand"),
               mapOf("name" to "jones", "country" to "New Zealand")
            )
         )

         // Even though we discovered twice, we should only have invoked this erroring service once, as the inputs are exactly the same
         invokedPaths["/country/nz/name"]!!.should.equal(1)
      }

   @Test
   fun `when service returns an http in a service in the middle of a discovery path then error subsequent attempts get the error replayed`(): Unit =
      runBlocking {
         val buildNewVyne = {
            testVyne(
               """
         model Person {
            name : Name inherits String
            country : CountryId inherits Int
         }
         model Country {
            @Id id : CountryId
            name : CountryName inherits String
         }
         service Service {
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/people")
            operation findPeople():Person[]
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country/{id}")
            operation findCountry(@PathVariable("id") id : CountryId):Country
         }
      """, Invoker.RestTemplateWithCache
            )
         }

         val invokedPaths = ConcurrentHashMap<String, Int>()
         // Test for multiple error codes
         listOf(400, 404, 500, 503).forEach { errorCode ->
            invokedPaths.clear()
            server.prepareResponse(
               invokedPaths,
               "/people" to response("""[ { "name" : "jimmy" , "country" : 1 }, {"name" : "jack", "country" : 1 }, {"name" : "jones", "country" : 1 }]"""),
               "/country/1" to response("", errorCode)
            )

            // Create a new vyne instance to destroy the cache between loops
            val result = buildNewVyne().query(
               """find { Person[] } as {
         personName : Name
         countryName : CountryName }[]"""
            )
               .typedObjects()

            // Should've only called once
            invokedPaths["/country/1"].should.equal(1)

            result.map { it["countryName"] }
               .forEach { countryName ->
                  countryName.source.failedAttempts.should.have.size(1)
                  countryName.source.failedAttempts.first().should.be.instanceof(OperationResultReference::class.java)

               }

         }
      }


   @Test
   @OptIn(ExperimentalTime::class)
   fun when_invokingService_then_itGetsInvokedCorrectly() {
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "stuff" : "Right back atcha, kid" }""")
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      val (context, events) = eventCapturingQueryContext()
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("vyne.ClientId", "myClientId", schema),
               paramAndType("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
            ), context, "testQuery", QueryOptions()
         ).testIn(this)

         val typedInstance = turbine.expectTypedObjectFromEither()
         expect(typedInstance.type.fullyQualifiedName).to.equal("vyne.CreditCostResponse")
         expect(typedInstance["stuff"].value).to.equal("Right back atcha, kid")
         turbine.awaitComplete()

         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/costs/myClientId/doCalculate", request.path)
            assertEquals(HttpMethod.POST.name(), request.method)
            assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
         }

         events.shouldHaveSize(1)
         val event = events.single()
         val headers = event.remoteCall.exchange
            .shouldBeInstanceOf<HttpExchange>()
            .headers
         headers.requestHeaders.shouldHaveSize(2)
         headers.responseHeaders.shouldHaveSize(2)
      }

   }

   @Test
   fun `When @OmitNulls annotation is set on parameter object null values are filtered`() {
      val testSchema = """
         namespace vyne {
             @com.orbitalhq.models.OmitNulls
             parameter model CreditScoreRequest {
                 clientId : ClientId
                 clientName: ClientName? inherits String
             }
             type ClientId inherits String

              model CreditScoreResponse {
                 score : CreditScore inherits Decimal
             }


             service CreditScoreService {
                 @HttpOperation(method = "POST",url = "http://localhost:{{PORT}}/score/doCalculate")
                 operation calculateCreditScore(@RequestBody CreditScoreRequest ) : CreditScoreResponse
             }

         }      """

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "score" : 90.9 }""")
      }

      val schema = TaxiSchema.from(testSchema.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.CreditScoreService")
      val operation = service.operation("calculateCreditScore")

      val (context, events) = eventCapturingQueryContext()
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("vyne.CreditScoreRequest", mapOf("clientId" to "123", "clientName" to null), schema)
            ), context, "testQuery", QueryOptions()
         ).testIn(this)

         val typedInstance = turbine.expectTypedObjectFromEither()
         expect(typedInstance.type.fullyQualifiedName).to.equal("vyne.CreditScoreResponse")
         val score = typedInstance["score"].value
         expect(score).to.equal(BigDecimal("90.9"))
         turbine.awaitComplete()

         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/score/doCalculate", request.path)
            assertEquals(HttpMethod.POST.name(), request.method)
            val body = String(request.body.readByteArray())
            assertEquals(body, """{"clientId":"123"}""")
            assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
         }

         events.shouldHaveSize(1)
         val event = events.single()
         val headers = event.remoteCall.exchange
            .shouldBeInstanceOf<HttpExchange>()
            .headers
         headers.requestHeaders.shouldHaveSize(2)
         headers.responseHeaders.shouldHaveSize(2)
      }
   }

   @Test
   fun `When @OmitNulls annotation is set on parameter object null values are filtered for object members`() {
      val testSchema = """
             @com.orbitalhq.models.OmitNulls
             parameter model OrderUpdateData {
                requestId: RequestId inherits String
                 details: {
                   name: OrderName? inherits String
                   id: OrderId? inherits String
               }
             }

             service OrderService {
                 @HttpOperation(method = "POST",url = "http://localhost:{{PORT}}/order/update")
                 operation updateOrder(@RequestBody OrderUpdateData ) : String
             }

        """

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""hello""")
      }

      val schema = TaxiSchema.from(testSchema.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      runTest {
         callOperation(
            schema,
            "OrderService",
            "updateOrder",
            listOf(
               paramAndType(
                  "OrderUpdateData",
                  mapOf("details" to mapOf<String, Any?>("name" to null, "id" to null), "requestId" to "req-1"),
                  schema
               )
            )
         )
         expectRequest { request ->
            val body = String(request.body.readByteArray())
            assertEquals("""{"requestId":"req-1"}""", body)
            assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
         }
      }
   }

   @Test
   fun `partials with nested object works with OmitNull`() {
      val testSchema = """
         closed parameter model Film {
            info : {
             title: Title inherits String
             director: Director inherits String
            }
            revenue : Revenue inherits Int
         }
        @com.orbitalhq.models.OmitNulls
         partial model FilmUpdate from Film
         service FilmsApi {
            operation getFilm():Film
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
        """
   }

   @Test
   fun `can project a 204 with empty body using a coalesce function`(): Unit = runBlocking {
      val vyne = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                @HttpOperation(method = "POST",url = "http://localhost:${server.port}/company/")
                operation getCompany(BorrowerId):CompanyMemberData
            }
         """.trimIndent(), Invoker.RestTemplate
      )

      val invokedPaths = ConcurrentHashMap<String, Int>()
      server.prepareResponse(
         invokedPaths,
         "/company" to emptyResponse()
      )

      val result = vyne.query(
         """given { accountId: BorrowerId = "626442000008369109" }
find { CompanyMemberData ?: {} } as {
    id : ContactId
    fullName: ContactName
}
         """.trimMargin()
      )
         .typedInstances()
      result.shouldHaveSize(1)
      result.map { it.toRawObject() }
         .shouldBe(listOf(mapOf("id" to null, "fullName" to null)))
   }

   @Test
   fun `can project a coalesced 204 with empty body where the function declares an array return type`(): Unit =
      runBlocking {
         val vyne = testVyne(
            """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                @HttpOperation(method = "POST",url = "http://localhost:${server.port}/company/")
                operation getCompany(BorrowerId):CompanyMemberData[]
            }
         """.trimIndent(), Invoker.RestTemplate
         )

         val invokedPaths = ConcurrentHashMap<String, Int>()
         server.prepareResponse(
            invokedPaths,
            "/company" to emptyResponse()
         )

         val result = vyne.query(
            """given { accountId: BorrowerId = "24601" }
find { CompanyMemberData[] ?:  [{}] } as {
    id : ContactId
    fullName: ContactName
}[]
         """.trimMargin()
         )
            .typedInstances()
         result.shouldHaveSize(1)
         result.map { it.toRawObject() }
            .shouldBe(listOf(mapOf("id" to null, "fullName" to null)))
      }

   @Test
   fun `can project a collection from http`(): Unit = runBlocking {
      val vyne = testVyne(
         """
            type BorrowerId inherits String

            model CompanyMemberData {
                contactId : ContactId inherits String
                fullName : ContactName inherits String
            }

            service CompanyApi {
                @HttpOperation(method = "POST",url = "http://localhost:${server.port}/company")
                operation getCompany(BorrowerId):CompanyMemberData[]
            }
         """.trimIndent(), Invoker.RestTemplate
      )

      val invokedPaths = ConcurrentHashMap<String, Int>()
      server.prepareResponse(
         invokedPaths,
         "/company" to response(
            """[
            |{ "contactId" : "123" , "fullName" : "Jimmy" },
            |{ "contactId" : "456" , "fullName" : "Jane" }
            | ]""".trimMargin()
         )
      )

      val result = vyne.query(
         """given { accountId: BorrowerId = "626442000008369109" }
find { CompanyMemberData[] } as {
    name: ContactName
}[]
         """.trimMargin()
      )
         .typedInstances()
      result
   }

   // ORB-981
   @Test
   fun `omit nulls defined in a parent parameter object is inherited onto children`(): Unit = runBlocking {
      val (vyne, stub) = com.orbitalhq.testVyne(
         """

//         @com.orbitalhq.models.OmitNulls
         model AccountBlock {
            clientRef: ClientRef inherits String
            primaryClientRef: PrimaryClientRef inherits String
            additionalData: AdditionalData inherits String
         }

         @com.orbitalhq.models.OmitNulls
         parameter model OnboardingRequest {
            accounts: AccountBlock[]
         }

         service OnboardingApi {
            write operation doOnboarding(OnboardingRequest):OnboardingRequest
         }
      """.trimIndent()
      )
      stub.addResponseReturningInputs("doOnboarding")
      val response = vyne.query(
         """
         import AccountBlock
         given { ClientRef = 'client1'}
         find { AccountBlock}
         call OnboardingApi::doOnboarding
      """.trimIndent()
      )
         .firstRawObject()
      val requestObject = stub.calls["doOnboarding"].single().single()
         .toRawObject()
      requestObject
         .shouldBe(
            mapOf(
               "accounts" to listOf(
                  mapOf("clientRef" to "client1") // primaryclientRef and additionalData are omitted
               )
            )
         )
   }

   private fun paramAndType(
      typeName: String,
      value: Any,
      schema: TaxiSchema,
      paramName: String? = null
   ): Pair<Parameter, TypedInstance> {
      val type = schema.type(typeName)
      return Parameter(type, paramName, nullable = false) to TypedInstance.from(type, value, schema, source = Provided)
   }

   private suspend fun callOperation(
      schema: TaxiSchema,
      serviceName: String,
      operationName: String,
      parameters: List<Pair<Parameter, TypedInstance>>
   ): List<Either<StreamErrorMessage, TypedInstance>> {
      val service = schema.service(serviceName);
      val operation = service.operation(operationName)
      val (context, _) = eventCapturingQueryContext()

      return RestTemplateInvoker(
         webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
         schemaProvider = SimpleSchemaProvider(schema)
      ).invoke(
         service,
         operation,
         parameters,
         context,
         "testQuery",
         QueryOptions()
      ).toList()
   }

   @Test
   @OptIn(ExperimentalTime::class)
   fun `attributes returned from service not defined in type are ignored`() {
      val responseJson = """{
         |"id" : 100,
         |"name" : "Fluffy"
         |}
      """.trimMargin()

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(responseJson)
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")
      val queryContext: QueryContext = eventCapturingQueryContext().first
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
            //SchemaProvider.from(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
            ), queryContext, "MOCK_QUERY_ID", QueryOptions()
         )
            .testIn(this)
         val typedInstance = turbine.expectTypedObjectFromEither()
         typedInstance["id"].value.should.equal(100)
         turbine.awaitComplete()

         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/pets/100", request.path)
            assertEquals(HttpMethod.GET.name(), request.method)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }
      }
   }

   @Test
   @OptIn(ExperimentalTime::class)
   fun `headers are captured on error result`() {
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setResponseCode(404)
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      val (context, events) = eventCapturingQueryContext()
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("vyne.ClientId", "myClientId", schema),
               paramAndType("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
            ), context, "testQuery", QueryOptions()
         ).testIn(this)
         val typedInstance = turbine.awaitError()

         expectRequestCount(1)
         events.shouldHaveSize(1)
         val event = events.single()
         val headers = event.remoteCall.exchange
            .shouldBeInstanceOf<HttpExchange>()
            .headers
         headers.requestHeaders.shouldHaveSize(2)
         headers.responseHeaders.shouldHaveSize(2)
      }

   }


   @Test
   @OptIn(ExperimentalTime::class)
   fun whenInvoking_paramsCanBePassedByTypeIfMatchedUnambiguously() {
      // This test is a WIP, that's been modified to pass.
      // This test is intended as a jumpting off point for issue #49
      // https://gitlab.com/vyne/vyne/issues/49
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "id" : 100 }""")
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}")).withBuiltIns()
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")
      val queryContext: QueryContext = eventCapturingQueryContext().first
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
            ), queryContext, "MOCK_QUERY_ID", QueryOptions()
         ).testIn(this)

         turbine.expectTypedObjectFromEither()
         turbine.awaitComplete()

         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/pets/100", request.path)
            assertEquals(HttpMethod.GET.name(), request.method)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }

      }


   }

   @Test
   @OptIn(ExperimentalTime::class)
   fun `when invoking a service with preparsed content then accessors are not evaluated`() {
      val responseJson = """{
         "id" : 100,
         "name" : "Fluffy"
         }
      """.trimMargin()

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setHeader(com.orbitalhq.http.HttpHeaderNames.CONTENT_PREPARSED, true.toString())
            .setBody(responseJson)
      }

      // Note: The jsonPaths are supposed to ignored, because the content is preparsed
      val schema = TaxiSchema.from(
         """
         service PetService {
            @HttpOperation(method = "GET",url = "http://localhost:${server.port}/pets")
            operation getBestPet():Animal
         }
         model Animal {
            id : String by jsonPath("$.animalsId")
            name : String by jsonPath("$.animalName")
         }
      """
      ).withBuiltIns()

      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")
      val queryContext: QueryContext = eventCapturingQueryContext().first
      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         )
            .invoke(service, operation, emptyList(), queryContext, "MOCK_QUERY_ID", QueryOptions())
            .testIn(this)
         val instance = turbine.expectTypedObjectFromEither()
         instance["id"].value.should.equal("100")
         instance["name"].value.should.equal("Fluffy")
         turbine.awaitComplete()
         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/pets", request.path)
            assertEquals(HttpMethod.GET.name(), request.method)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }
      }
   }

   @OptIn(ExperimentalTime::class)
   @Test
   fun `when invoking a service without preparsed content then accessors are not evaluated`() {
      val responseJson = """{
         "animalsId": 100,
         "animalName": "Fluffy"
         }
      """.trimMargin()

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody(responseJson)
      }


      // Note: The jsonPaths are supposed to ignored, because the content is preparsed
      val schema = TaxiSchema.from(
         """
         service PetService {
            @HttpOperation(method = "GET",url = "http://localhost:${server.port}/pets")
            operation getBestPet():Animal
         }
         model Animal {
            id : String by jsonPath("$.animalsId")
            name : String by jsonPath("$.animalName")
         }
      """
      ).withBuiltIns()

      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")
      val queryContext: QueryContext = eventCapturingQueryContext().first

      runTest {
         val turbine = RestTemplateInvoker(
            webClientFactory = WebClientFactory(WebClient.builder(), AuthWebClientCustomizer.empty()),
            schemaProvider = SimpleSchemaProvider(schema)
         )
            .invoke(service, operation, emptyList(), queryContext, "MOCK_QUERY_ID", QueryOptions())
            .testIn(this)

         val instance = turbine.expectTypedObjectFromEither()
         instance["id"].value.should.equal("100")
         instance["name"].value.should.equal("Fluffy")
         turbine.awaitComplete()
         expectRequestCount(1)
         expectRequest { request ->
            assertEquals("/pets", request.path)
            assertEquals(HttpMethod.GET.name(), request.method)
            // There is no request body and hence we don't set the content-type header for the request.
            assertEquals(null, request.getHeader("Content-Type"))
         }
      }
   }


   @Test
   fun `large result set performance test`(): Unit = runBlocking {
      val recordCount = 5000

      val vyne = testVyne(
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

      Benchmark.benchmark("Heavy load", warmup = 0, iterations = 1) {
         runBlocking {
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

            val result = vyne.query(
               """find { Movie[] } as {
         title : MovieTitle
         director : DirectorName
         producer : ProductionCompanyName
         rating : MovieRating
         country : CountryName
         }[]
      """
            ).typedObjects()
            result.should.have.size(recordCount)
         }

      }

      val stats = StrategyPerformanceProfiler.summarizeAndReset().sortedByCostDesc()
      logger.warn("Perf test of $recordCount completed")
      logger.warn("Stats:\n ${jackson.writerWithDefaultPrettyPrinter().writeValueAsString(stats)}")
   }

   @Test
   fun `can resolve queryString parameters from facts`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")

      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `can resolve query variables from facts`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation listPeople(@taxi.http.QueryVariable(value = "apiKey") apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `matches path variables based on annotation name`():Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people/{personId}")
            operation listPeople(@PathVariable(value = "personId") thePersonId:PersonId):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )
   }
   @Test
   fun `matches path variables based on annotation name when operation doesnt have a variable name`():Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            id : PersonId inherits String
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people/{personId}")
            // Note that the PersonId param isn't named
            operation listPeople(@PathVariable(value = "personId") PersonId):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""given { id: PersonId = "123" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people/123", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `when query variables are optional then it is valid not to supply them`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            operation listPeople(@taxi.http.QueryVariable(value = "apiKey") apiKey:ApiKey?):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `when query variables are optional and supplied then they are populated`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            // THIS IS THE TEST: Note that ApiKey is optional
            operation listPeople(@taxi.http.QueryVariable("apiKey") apiKey:ApiKey?):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }


   @Test
   fun `does not use primitives to populate query params`() = runBlocking {
      val vyne = testVyne(
         """
            type ApiKey inherits String
            model Person {
               name : Name inherits String
            }

            service PersonService {
               @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
               operation listPeople(
                  @taxi.http.QueryVariable(value = "apiKey") apiKey: ApiKey,
                  // This should not be populated
                  @taxi.http.QueryVariable(value = "sortOrder") sortOrder: String?
               ):Person[]
            }
         """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }

   }


   @Test
   fun `request body is populated on request`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type PersonId inherits String
         parameter model PersonRequest {
            id : PersonId
         }
         model Person {
            name : Name inherits String
         }
          service PersonService {
            @HttpOperation(method = "POST", url = "http://localhost:${server.port}/person")
            operation getPerson(@RequestBody PersonRequest):Person
          }
      """, invoker = Invoker.RestTemplate
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "name" : "Jimmy" }""")
      }
      vyne.query("""given { PersonId = "1" } find { Person }""")
         .rawObjects()
      expectRequestCount(1)
      expectRequest { request ->
         assertEquals(HttpMethod.POST.name(), request.method)
         val requestBody = String(request.body.readByteArray())
         requestBody.shouldBe("""{"id":"1"}""")
      }

   }

   @Test
   fun `can resolve path variables from facts`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/{apiKey}/people")
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/hello/people", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test

   fun `http urls resolve against base url`(): Unit = runBlocking {
      val vyne = vyneWithHttpInvoker(
         """
         model Person {
            name : Name inherits String
         }
         @HttpService(baseUrl="http://localhost:${server.port}")
         service PeopleService {
            @HttpOperation(method = "GET", url = "/people")
            operation findPeople():Person[]
         }
      """.trimIndent()
      )
      server.addJsonResponse("""[ { "name" : "Jimmy" } ]""")
      server.requestCount.shouldBe(0)
      vyne.query(
         """find { Person[] } """
      )
         .rawObjects()
      server.requestCount.shouldBe(1)
   }

   @Test
   fun `can use fixed retry policy`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            @HttpRetry(responseCode = [502, 503], fixedRetryPolicy = @HttpFixedRetryPolicy(maxRetries = 2, retryDelay = 1))
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(503)

      }
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(502)


      }

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")

      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(3)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `when server call fails because server unreachable then trace events are captured`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
         // Note the intenitonally bad port - this request will fail
            @HttpOperation(method = "GET", url = "http://localhost:123/people?apiKey={apiKey}")
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()
      assertThrows<OperationInvocationException> {
         vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""", eventBroker = eventBroker)
            .rawObjects()
      }

      eventSink.collectedEvents.shouldHaveSize(2)
      eventSink.collectedEvents[0].exchangeMetadata.shouldBeInstanceOf<HttpRequest>()
      eventSink.collectedEvents[1].exchangeMetadata.shouldBeInstanceOf<ConnectionError>()
   }

   @Test
   fun `when server calls are retried then trace events are captured`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            @HttpRetry(responseCode = [502, 503], fixedRetryPolicy = @HttpFixedRetryPolicy(maxRetries = 2, retryDelay = 1))
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(503)

      }
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(502)


      }

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
      }
      val eventSink = CollectingEventSink()
      val eventBroker = QueryContextEventBroker(traceSpan = TraceContext.newTrace("fake-query-id", eventSink).rootSpan)
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""", eventBroker = eventBroker)
         .rawObjects()

      eventSink.collectedEvents.shouldHaveSize(6)
      val eventMetadata = eventSink.collectedEvents.map { it.exchangeMetadata }
      eventMetadata[0].shouldBeHttpRequest()
      eventSink.collectedEvents[0].tracingEventKind.shouldBe(TracingEventKind.OK)
      eventMetadata[1].shouldBeHttpResponseWithStatusCode(503)
      eventSink.collectedEvents[1].tracingEventKind.shouldBe(TracingEventKind.ERROR)
      eventMetadata[2].shouldBeHttpRequest()
      eventMetadata[3].shouldBeHttpResponseWithStatusCode(502)
      eventSink.collectedEvents[3].tracingEventKind.shouldBe(TracingEventKind.ERROR)
      eventMetadata[4].shouldBeHttpRequest()
      eventMetadata[5].shouldBeHttpResponseWithStatusCode(200)
      eventSink.collectedEvents[5].tracingEventKind.shouldBe(TracingEventKind.OK)
   }

   @Test
   fun `instances from HTTP requests are linked to source events`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" },  { "name" : "Jack" }]""")
      }
      val (eventBroker, _) = QueryContextEventBroker.withTestTraceSpan()
      val typedInstances =
         vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""", eventBroker = eventBroker)
            .typedInstances()

      // Verify that the instances, when emitted, were linked back to their source events
      typedInstances[0].source
         .shouldBeInstanceOf<OperationResultReference>()
         .sourceEventId.shouldNotBeNull()
      typedInstances[1].source
         .shouldBeInstanceOf<OperationResultReference>()
         .sourceEventId.shouldNotBeNull()
   }

   @Test
   fun `can use exponential retry policy`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            @HttpRetry(responseCode = [502, 503], exponentialRetryPolicy = @HttpExponentialRetryPolicy(maxRetries = 2, retryDelay = 1, jitter = 0.75))
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(503)

      }
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            .setResponseCode(502)


      }

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")

      }
      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(3)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `error is not swallowed when retry policy is exhausted`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people")
            @HttpRetry(responseCode = [502, 503], exponentialRetryPolicy = @HttpExponentialRetryPolicy(maxRetries = 2, retryDelay = 1, jitter = 0.75))
            operation listPeople():Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
            .setResponseCode(503)

      }
      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
            .setResponseCode(502)
      }

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
            .setResponseCode(502)

      }

      val remoteCalls = object : RemoteCallOperationResultHandler {
         val operationResults = mutableListOf<OperationResult>()
         override fun recordResult(operation: OperationResult, queryId: String) {
            operationResults.add(operation)
         }
      }

      val queryEventBroker = QueryContextEventBroker(traceSpan = TraceContext.noOp().rootSpan)
      queryEventBroker.addHandler(remoteCalls)

      var queryException: Exception? = null
      val queryResult = vyne.query(
         vyneQlQuery = """
         find { Person[] }
      """.trimMargin(),
         eventBroker = queryEventBroker
      )

      try {
         queryResult.rawObjects()
      } catch (e: Exception) {
         queryException = e
      }

      queryException.should.be.instanceof(OperationInvocationException::class.java)
      remoteCalls.operationResults.size.should.equal(3)
      val resultCodes = remoteCalls.operationResults.map { it.remoteCall.resultCode }
      resultCodes.count { it == 502 }.should.equal(2)
      resultCodes.count { it == 503 }.should.equal(1)
   }

   @Test
   fun `will not retry if the received error does not match retry codes`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            @HttpRetry(responseCode = [502, 503], fixedRetryPolicy = @HttpFixedRetryPolicy(maxRetries = 2, retryDelay = 1))
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{}""")
            // We should not retry as the server returns 501.
            .setResponseCode(501)

      }

      val remoteCalls = object : RemoteCallOperationResultHandler {
         val operationResults = mutableListOf<OperationResult>()
         override fun recordResult(operation: OperationResult, queryId: String) {
            operationResults.add(operation)
         }
      }

      val queryEventBroker = QueryContextEventBroker(traceSpan = TraceContext.noOp().rootSpan)
      queryEventBroker.addHandler(remoteCalls)

      val queryResults = vyne.query(
         vyneQlQuery = """given { key : ApiKey = "hello" } find { Person[] }""",
         eventBroker = queryEventBroker
      )

      var queryException: Exception? = null
      try {
         queryResults.rawObjects()
      } catch (e: Exception) {
         queryException = e
      }
      queryException.should.be.instanceof(OperationInvocationException::class.java)
      remoteCalls.operationResults.first().remoteCall.resultCode.should.equal(501)
      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   @Test
   fun `can use HttpIgnoreErrors to return result from responses with Http error status`(): Unit = runBlocking {
      val vyne = testVyne(
         """
         type ApiKey inherits String
         model Person {
            name : Name inherits String
         }
         service PersonService {
            @HttpOperation(method = "GET", url = "http://localhost:${server.port}/people?apiKey={apiKey}")
            @HttpIgnoreErrors(responseCodes = ["5xx"])
            operation listPeople(apiKey:ApiKey):Person[]
          }
      """, invoker = Invoker.RestTemplate
      )

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""[ { "name" : "Jimmy" }]""")
            .setResponseCode(501)

      }

      vyne.query("""given { key : ApiKey = "hello" } find { Person[] }""")
         .rawObjects()

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people?apiKey=hello", request.path)
         assertEquals(HttpMethod.GET.name(), request.method)
      }
   }

   fun eventCapturingQueryContext(): Pair<QueryContext, MutableList<OperationResult>> {
      val operationResults = mutableListOf<OperationResult>()
      val context = mock<QueryContext> {
         val capture = argumentCaptor<OperationResult>()
         val serviceCapture = argumentCaptor<Service>()
         val operationCapture = argumentCaptor<RemoteOperation>()
         val resourceNameCapture = argumentCaptor<String>()
         val remoteCallIdCapture = argumentCaptor<String>()
         on {
            createOperationTraceSpan(
               serviceCapture.capture(),
               operationCapture.capture(),
               resourceNameCapture.capture(),
               remoteCallIdCapture.capture()
            )
         } doAnswer {
            OperationTraceSpan(
               TraceContext.noOp().rootSpan,
               serviceCapture.lastValue,
               operationCapture.lastValue,
               resourceNameCapture.lastValue,
               remoteCallIdCapture.lastValue
            )
         }
         on { reportRemoteOperationInvoked(capture.capture(), com.nhaarman.mockito_kotlin.any()) } doAnswer {
            operationResults.add(capture.lastValue)
            Unit

         }
      }
      return context to operationResults
   }
}

private fun TracingEventExchangeMetadata.shouldBeHttpRequest(): HttpRequest {
   return this.shouldBeInstanceOf<HttpRequest>()

}

private fun TracingEventExchangeMetadata.shouldBeHttpResponseWithStatusCode(statusCode: Int): HttpResponse {
   this.shouldBeInstanceOf<HttpResponse>()
      .responseCode.shouldBe(statusCode)
   return this

}
