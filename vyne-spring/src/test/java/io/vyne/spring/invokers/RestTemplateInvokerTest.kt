package io.vyne.spring.invokers

import app.cash.turbine.test
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.expectTypedObject
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.QueryProfiler
import io.vyne.query.active.ActiveQueryMonitor
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Before
import java.util.function.Consumer
import kotlin.test.assertEquals
import okhttp3.mockwebserver.RecordedRequest
import kotlin.time.ExperimentalTime
import kotlin.time.seconds


@ExperimentalTime
class RestTemplateInvokerTest {

   var server = MockWebServer()

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
   }

   @After
   fun stopServer() {
      server.shutdown()
   }

   @Test
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

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema),
            activeQueryMonitor = ActiveQueryMonitor()
         )
            .invoke(
               service, operation, listOf(
                  paramAndType("vyne.ClientName", "notional", schema)
               ), QueryProfiler(), "MOCK_QUERY_ID"
            ) .test(timeout = 5.seconds) {
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

      val webClient = WebClient.builder().build()

      val json = """
         [{ "firstName" : "Jimmy", "lastName" : "Pitt", "id" : "123" }]
      """.trimIndent()

      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json) }

      val schema = TaxiSchema.from(
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
      """
      )

      val restTemplateInvoker = RestTemplateInvoker(
         webClient = webClient,
         schemaProvider = SchemaProvider.from(schema),
         activeQueryMonitor = ActiveQueryMonitor()
      )
      val vyne = testVyne(schema, listOf(restTemplateInvoker))

      runBlocking {
         val response = vyne.query("findAll { Person[] }")
         response.isFullyResolved.should.be.`true`
         response.results.test(timeout = 5.seconds) {
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
   fun when_invokingService_then_itGetsInvokedCorrectly() {

      val webClient = WebClient.builder()
         .build()

      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "stuff" : "Right back atcha, kid" }""") }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      runBlocking {
         RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema),
            activeQueryMonitor = ActiveQueryMonitor()
         ).invoke(
            service, operation, listOf(
               paramAndType("vyne.ClientId", "myClientId", schema),
               paramAndType("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
            ), QueryProfiler()
         ).test(timeout = 5.seconds) {
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
   fun `attributes returned from service not defined in type are ignored`()  {

      val webClient = WebClient.builder().build()

      val responseJson = """{
         |"id" : 100,
         |"name" : "Fluffy"
         |}
      """.trimMargin()

      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(responseJson) }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      runBlocking {
         val invoker = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema),
            activeQueryMonitor = ActiveQueryMonitor()
         )
         invoker
            .invoke(
               service, operation, listOf(
                  paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
               ), QueryProfiler(), "MOCK_QUERY_ID"
            ).test(timeout = 5.seconds) {
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
   fun whenInvoking_paramsCanBePassedByTypeIfMatchedUnambiguously() {
      // This test is a WIP, that's been modified to pass.
      // This test is intended as a jumpting off point for issue #49
      // https://gitlab.com/vyne/vyne/issues/49

      val webClient = WebClient.builder().build()

      prepareResponse { response -> response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody("""{ "id" : 100 }""") }

      val schema = TaxiSchema.from(taxiDef.replace("{{PORT}}", "${server.port}"))
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      runBlocking {
         val response = RestTemplateInvoker(
            webClient = webClient,
            schemaProvider = SchemaProvider.from(schema),
            activeQueryMonitor = ActiveQueryMonitor()
         ).invoke(
            service, operation, listOf(
               paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
            ), QueryProfiler(), "MOCK_QUERY_ID"
         ).test(timeout = 5.seconds) {
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

   @ExperimentalTime
   @Test
   fun `when invoking a service with preparsed content then accessors are not evaluated`() {

      val webClient = WebClient.builder().build()
      val responseJson = """{
         "id" : 100,
         "name" : "Fluffy"
         }
      """.trimMargin()

      prepareResponse { response ->
         response.setHeader("Content-Type",MediaType.APPLICATION_JSON)
            .setHeader(io.vyne.http.HttpHeaders.CONTENT_PREPARSED,true.toString())
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
            schemaProvider = schemaProvider,
            activeQueryMonitor = ActiveQueryMonitor()
         )
            .invoke(service, operation, emptyList(), QueryProfiler(), "MOCK_QUERY_ID").test(timeout = 5.seconds) {
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

   @ExperimentalTime
   @Test
   fun `when invoking a service without preparsed content then accessors are not evaluated`() {

      val webClient = WebClient.builder().build()
      val responseJson = """{
         "animalsId": 100,
         "animalName": "Fluffy"
         }
      """.trimMargin()

      prepareResponse { response ->
         response.setHeader("Content-Type",MediaType.APPLICATION_JSON)
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
            schemaProvider = schemaProvider,
            activeQueryMonitor = ActiveQueryMonitor()
         )
            .invoke(service, operation, emptyList(), QueryProfiler(), "MOCK_QUERY_ID").test(timeout = 5.seconds) {
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
