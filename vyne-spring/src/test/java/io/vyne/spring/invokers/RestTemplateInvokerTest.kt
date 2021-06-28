package io.vyne.spring.invokers

import app.cash.turbine.test
import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.expectTypedObject
import io.vyne.http.MockWebServerRule
import io.vyne.models.OperationResult
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.rawObjects
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}
// See also VyneQueryTest for more tests related to invoking Http services
@OptIn(kotlin.time.ExperimentalTime::class)
class RestTemplateInvokerTest {

   @Rule
   @JvmField
   val server = MockWebServerRule()

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


      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json)
      }

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

      server.prepareResponse { response ->
         response.setHeader("Content-Type", MediaType.APPLICATION_JSON).setBody(json)
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
            @HttpOperation(method = "GET" , url = "http://localhost:${server.port}/country/{id}")
            operation findCountry(@PathVariable("id") id : CountryId):Country
         }
      """, Invoker.RestTemplateWithCache
         )
      }

      // Test for multiple error codes
      val invokedPaths: MutableMap<String, Int> = mutableMapOf()
      listOf(400, 404, 500, 503).forEach { errorCode ->
         invokedPaths.clear()
         server.prepareResponse(
            invokedPaths,
            "/people" to response("""[ { "name" : "jimmy" , "country" : 1 }, {"name" : "jack", "country" : 1 }, {"name" : "jones", "country" : 1 }]"""),
            "/country/1" to response("", errorCode)
         )

         // Create a new vyne instance to destroy the cache between loops
         val result = buildNewVyne().query(
            """findAll { Person[] } as {
         personName : Name
         countryName : CountryName }[]"""
         )
            .typedObjects()

         // Should've only called once
         invokedPaths["/country/1"].should.equal(1)

         result.map { it["countryName"] }
            .forEach { countryName ->
               countryName.source.failedAttempts.should.have.size(1)
               countryName.source.failedAttempts.first().should.be.instanceof(OperationResult::class.java)

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
         val invokedPaths: MutableMap<String, Int> = mutableMapOf()
         server.prepareResponse(
            invokedPaths,
            "/people" to response("""[ { "name" : "jimmy" , "countryId" : "nz"  } , {"name": "jones", "countryId" : "nz" }]"""),
            "/country/nz/name" to response("Unknown country id", 404),
            "/country/nz/isoCode" to response("NZD"),
            "/country/iso/NZD/name" to response("New Zealand")
         )

         val response = vyne.query("""findAll { Person[] } as { name : Name country : CountryName }[]""")
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

         val invokedPaths: MutableMap<String, Int> = mutableMapOf()
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
               """findAll { Person[] } as {
         personName : Name
         countryName : CountryName }[]"""
            )
               .typedObjects()

            // Should've only called once
            invokedPaths["/country/1"].should.equal(1)

            result.map { it["countryName"] }
               .forEach { countryName ->
                  countryName.source.failedAttempts.should.have.size(1)
                  countryName.source.failedAttempts.first().should.be.instanceof(OperationResult::class.java)

               }

         }
      }


   private fun response(body: String, responseCode: Int = 200, contentType:MediaType = MediaType.APPLICATION_JSON): () -> MockResponse {
      return {
         MockResponse().setHeader("Content-Type", contentType).setBody(body)
            .setResponseCode(responseCode)
      }
   }

   @Test
   @OptIn(kotlin.time.ExperimentalTime::class)
   fun when_invokingService_then_itGetsInvokedCorrectly() {

      val webClient = WebClient.builder()
         .build()

      server.prepareResponse { response ->
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

      server.prepareResponse { response ->
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

      server.prepareResponse { response ->
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

      server.prepareResponse { response ->
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
