package io.vyne.spring.invokers

import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.expectTypedObject
import io.vyne.models.OperationResult
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.time.Duration

@OptIn(kotlin.time.ExperimentalTime::class)
class RestTemplateInvokerTest {

   var server = MockWebServer()
   lateinit var invokedPaths :MutableMap<String,Int>
   val taxiDef = """
namespace vyne {

    type CreditCostRequest {
        deets : String
    }

    type alias ClientId as String

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


    @ServiceDiscoveryClient(serviceName = "localhost:{{PORT}}")
    service CreditCostService {
        @HttpOperation(method = "POST",url = "/costs/{vyne.ClientId}/doCalculate")
        operation calculateCreditCosts(@RequestBody CreditCostRequest, ClientId ) : CreditCostResponse
    }

    service PetService {
      @HttpOperation(method = "GET",url = "http://localhost:{{PORT}}/pets/{petId}")
      operation getPetById( petId : Int ):Pet
    }

    @ServiceDiscoveryClient(serviceName = "localhost:{{PORT}}")
    service ClientDataService {
        @HttpOperation(method = "GET",url = "/clients/{vyne.ClientName}")
        operation getContactsForClient( clientName: String ) : Client
    }
}      """


   private fun prepareResponse(consumer: Consumer<MockResponse>) {
      val response = MockResponse()
      consumer.accept(response)
      server.enqueue(response)
   }


   private fun prepareResponse(vararg responses: Pair<String, () -> MockResponse>) {
      server.dispatcher = object : Dispatcher() {
         override fun dispatch(request: RecordedRequest): MockResponse {
            invokedPaths.compute(request.path!!) { key, value ->
               if (value == null) 1 else value + 1
            }
            val handler =
               responses.firstOrNull { request.path == it.first } ?: error("No handler for path ${request.path}")
            return handler.second.invoke()
         }

      }
   }

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

   @Before
   fun startServer() {
      server = MockWebServer()
      invokedPaths = mutableMapOf()
   }

   @After
   fun stopServer() {
      server.shutdown()
   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun `When invoked a service that returns a list property mapped to a taxi array`() {

      val webClient = WebClient.builder().build()

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


      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json) }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.ClientDataService")
      val operation = service.operation("getContactsForClient")
      val queryContext: QueryContext = mock { }

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema)
         )
            .invoke(
               service, operation, listOf(
                  paramAndType("vyne.ClientName", "notional", schema)
               ), queryContext, "MOCK_QUERY_ID"
            ).test(Duration.ZERO) {
               val instance = expectTypedObject()
               expect(instance.type.fullyQualifiedName).to.equal("vyne.Client")
               expect(instance["name"].value).to.equal("Notional")
               expect((instance["contacts"] as TypedCollection)).size.to.equal(2)
               expectComplete()
            }
      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/clients/notional", request.path)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }

   }


   @Test
   fun `invoke a restTemplate from vyne`() {
      val json = """
         [{ "firstName" : "Jimmy", "lastName" : "Pitt", "id" : "123" }]
      """.trimIndent()

      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json) }

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
      """, Invoker.CachingInvoker
      )

      runBlocking {
         val response = vyne.query("findAll { Person[] }")
         response.isFullyResolved.should.be.`true`
         response.results.test(Duration.ZERO) {
            expectItem()
            expectComplete()
         }

      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/people", request.path)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }
   }

   @Test
   fun `when service returns an error subsequent attempts get the error replayed`(): Unit = runBlocking {
      val vyne = testVyne(
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
      """, Invoker.CachingInvoker
      )

      prepareResponse(
         "/people" to response("""[ { "name" : "jimmy" , "country" : 1 }, {"name" : "jack", "country" : 1 }]"""),
         "/country/1" to response("", 404)
      )

      val result = vyne.query("""findAll { Person[] } as {
         personName : Name
         countryName : CountryName }[]""")
         .typedObjects()

      // Should've only called once
      invokedPaths["/country/1"].should.equal(1)

      result.map { it["countryName"] }
         .forEach { countryName ->
            countryName.source.failedAttempts.should.have.size(1)
            countryName.source.failedAttempts.first().should.be.instanceof(OperationResult::class.java)
         }
   }

   private fun response(body: String, responseCode: Int = 200): () -> MockResponse {
      return {
         MockResponse().setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(body)
            .setResponseCode(responseCode)
      }
   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun when_invokingService_then_itGetsInvokedCorrectly() {

      val webClient = WebClient.builder()
         .build()

      prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setBody("""{ "stuff" : "Right back atcha, kid" }""")
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      runBlocking {
         RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("vyne.ClientId", "myClientId", schema),
               paramAndType("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
            ), mock { }
         ).test(Duration.ZERO) {
            val typedInstance = expectTypedObject()
            expect(typedInstance.type.fullyQualifiedName).to.equal("vyne.CreditCostResponse")
            expect(typedInstance["stuff"].value).to.equal("Right back atcha, kid")
            expectComplete()
         }

      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/costs/myClientId/doCalculate", request.path)
         assertEquals(HttpMethod.POST.name, request.method)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }

   }

   private fun paramAndType(
      typeName: String,
      value: Any,
      schema: TaxiSchema,
      paramName: String? = null
   ): Pair<Parameter, TypedInstance> {
      val type = schema.type(typeName)
      return Parameter(type, paramName) to TypedInstance.from(type, value, schema, source = Provided)
   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun `attributes returned from service not defined in type are ignored`() {

      val webClient = WebClient.builder().build()

      val responseJson = """{
         |"id" : 100,
         |"name" : "Fluffy"
         |}
      """.trimMargin()

      prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(responseJson)
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      runBlocking {
         val invoker = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema)
         )
         invoker
            .invoke(
               service, operation, listOf(
                  paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
               ), mock { }, "MOCK_QUERY_ID"
            ).test(Duration.ZERO) {
               val typedInstance = expectTypedObject()
               typedInstance["id"].value.should.equal(100)
               expectComplete()
            }

      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/pets/100", request.path)
         assertEquals(HttpMethod.GET.name, request.method)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }
   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun whenInvoking_paramsCanBePassedByTypeIfMatchedUnambiguously() {
      // This test is a WIP, that's been modified to pass.
      // This test is intended as a jumpting off point for issue #49
      // https://gitlab.com/vyne/vyne/issues/49

      val webClient = WebClient.builder().build()

      prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "id" : 100 }""")
      }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema)
         ).invoke(
            service, operation, listOf(
               paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
            ), mock { }, "MOCK_QUERY_ID"
         ).test(Duration.ZERO) {
            expectTypedObject()
            expectComplete()
         }
      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/pets/100", request.path)
         assertEquals(HttpMethod.GET.name, request.method)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }

   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun `when invoking a service with preparsed content then accessors are not evaluated`() {

      val webClient = WebClient.builder().build()
      val responseJson = """{
         "id" : 100,
         "name" : "Fluffy"
         }
      """.trimMargin()

      prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON)
            .setHeader(io.vyne.http.HttpHeaders.CONTENT_PREPARSED, true.toString())
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
      )

      val schemaProvider = SchemaProvider.from(schema)
      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = schemaProvider
         )
            .invoke(service, operation, emptyList(), mock { }, "MOCK_QUERY_ID").test(Duration.ZERO) {
               val instance = expectTypedObject()
               instance["id"].value.should.equal("100")
               instance["name"].value.should.equal("Fluffy")
               expectComplete()
            }

      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/pets", request.path)
         assertEquals(HttpMethod.GET.name, request.method)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }
   }

   @OptIn(kotlin.time.ExperimentalTime::class)
   @Test
   fun `when invoking a service without preparsed content then accessors are not evaluated`() {

      val webClient = WebClient.builder().build()
      val responseJson = """{
         "animalsId": 100,
         "animalName": "Fluffy"
         }
      """.trimMargin()

      prepareResponse { response ->
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
      )

      val schemaProvider = SchemaProvider.from(schema)
      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = schemaProvider
         )
            .invoke(service, operation, emptyList(), mock { }, "MOCK_QUERY_ID").test(Duration.ZERO) {
               val instance = expectTypedObject()
               instance["id"].value.should.equal("100")
               instance["name"].value.should.equal("Fluffy")
               expectComplete()
            }

      }

      expectRequestCount(1)
      expectRequest { request ->
         assertEquals("/pets", request.path)
         assertEquals(HttpMethod.GET.name, request.method)
         assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader("Content-Type"))
      }
   }
}
