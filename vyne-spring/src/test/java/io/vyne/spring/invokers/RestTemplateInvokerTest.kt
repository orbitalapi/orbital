package io.vyne.spring.invokers

import com.jayway.jsonpath.JsonPath
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.QueryProfiler
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.taxi.TaxiSchema
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient

class RestTemplateInvokerTest {
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


    @ServiceDiscoveryClient(serviceName = "mockService")
    service CreditCostService {
        @HttpOperation(method = "POST",url = "/costs/{vyne.ClientId}/doCalculate")
        operation calculateCreditCosts(@RequestBody CreditCostRequest, ClientId ) : CreditCostResponse
    }

    service PetService {
      @HttpOperation(method = "GET",url = "http://pets.com/pets/{petId}")
      operation getPetById( petId : Int ):Pet
    }

    @ServiceDiscoveryClient(serviceName = "clientService")
    service ClientDataService {
        @HttpOperation(method = "GET",url = "/clients/{vyne.ClientName}")
        operation getContactsForClient( clientName: String ) : Client
    }
}      """

   fun isJsonSatisfying(jsonPath: String, expectedValue: Any): Matcher<String> {
      return object : BaseMatcher<String>() {
         private var actualBody: String? = null
         override fun describeTo(desc: Description) {
            desc.appendText("Expected json matching $jsonPath, but got the following: $actualBody!!")
         }

         override fun matches(body: Any): Boolean {
            actualBody = body as String
            val result = JsonPath.read<Any>(body, jsonPath)
            expect(result).to.equal(expectedValue)
            return true
         }

      }
   }

   @Test
   fun `When invoked a service that returns a list property mapped to a taxi array`() {
      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()

      server.expect(ExpectedCount.once(), requestTo("http://clientService/clients/notional"))
         .andExpect(method(HttpMethod.GET))
         .andRespond(MockRestResponseCreators.withSuccess(
            """
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
           }""".trimIndent(),
            MediaType.APPLICATION_JSON))
      val schema = TaxiSchema.from(taxiDef)
      val service = schema.service("vyne.ClientDataService")
      val operation = service.operation("getContactsForClient")

      val response = RestTemplateInvoker(webClient = webClient, schemaProvider = SchemaProvider.from(schema), enableDataLineageForRemoteCalls = true)
         .invoke(service, operation, listOf(
         paramAndType("vyne.ClientName", "notional", schema)
      ), QueryProfiler()) as TypedObject
      expect(response.type.fullyQualifiedName).to.equal("vyne.Client")
      expect(response["name"].value).to.equal("Notional")
      expect((response["contacts"] as TypedCollection)).size.to.equal(2)
   }

   @Test
   fun when_invokingService_then_itGetsInvokedCorrectly() {
      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()

      server.expect(ExpectedCount.once(), requestTo("http://mockService/costs/myClientId/doCalculate"))
         .andExpect(method(HttpMethod.POST))
         .andExpect(content().string(isJsonSatisfying("$.deets", "Hello, world")))
         .andRespond(MockRestResponseCreators.withSuccess("""{ "stuff" : "Right back atcha, kid" }""", MediaType.APPLICATION_JSON))
      val schema = TaxiSchema.from(taxiDef)
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      val response = RestTemplateInvoker(webClient = webClient, schemaProvider = SchemaProvider.from(schema), enableDataLineageForRemoteCalls = true).invoke(service, operation, listOf(
         paramAndType("vyne.ClientId", "myClientId", schema),
         paramAndType("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
      ), QueryProfiler()) as TypedObject
      expect(response.type.fullyQualifiedName).to.equal("vyne.CreditCostResponse")
      expect(response["stuff"].value).to.equal("Right back atcha, kid")
   }

   private fun paramAndType(typeName: String, value: Any, schema: TaxiSchema, paramName:String? = null): Pair<Parameter, TypedInstance> {
      val type = schema.type(typeName)
      return Parameter(type,paramName) to TypedInstance.from(type, value, schema, source = Provided)
   }

   @Test
   fun `attributes returned from service not defined in type are ignored`() {
      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()
      val responseJson = """{
         |"id" : 100,
         |"name" : "Fluffy"
         |}
      """.trimMargin()
      server.expect(ExpectedCount.once(), requestTo("http://pets.com/pets/100"))
         .andExpect(method(HttpMethod.GET))
         .andRespond(MockRestResponseCreators.withSuccess(responseJson, MediaType.APPLICATION_JSON))

      val schema = TaxiSchema.from(taxiDef)
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      val invoker = RestTemplateInvoker(
         webClient = webClient,
         serviceUrlResolvers = listOf(AbsoluteUrlResolver()),
         enableDataLineageForRemoteCalls = true,
         schemaProvider = SchemaProvider.from(schema))
      val response = invoker
         .invoke(service, operation, listOf(
         paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
      ), QueryProfiler()) as TypedObject

      response["id"].value.should.equal(100)
   }

   @Test
   fun whenInvoking_paramsCanBePassedByTypeIfMatchedUnambiguously() {
      // This test is a WIP, that's been modified to pass.
      // This test is intended as a jumpting off point for issue #49
      // https://gitlab.com/vyne/vyne/issues/49

      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()
      server.expect(ExpectedCount.once(), requestTo("http://pets.com/pets/100"))
         .andExpect(method(HttpMethod.GET))
         .andRespond(MockRestResponseCreators.withSuccess("""{ "id" : 100 }""", MediaType.APPLICATION_JSON))

      val schema = TaxiSchema.from(taxiDef)
      val service = schema.service("vyne.PetService")
      val operation = service.operation("getPetById")

      val response = RestTemplateInvoker(
         webClient = webClient,
         serviceUrlResolvers = listOf(AbsoluteUrlResolver()),
         enableDataLineageForRemoteCalls = true,
         schemaProvider = SchemaProvider.from(schema)).invoke(service, operation, listOf(
         paramAndType("lang.taxi.Int", 100, schema, paramName = "petId")
      ), QueryProfiler()) as TypedObject

   }

   @Test
   fun `when invoking a service with preparsed content then accessors are not evaluated`() {
      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()
      val responseJson = """{
         |"id" : 100,
         |"name" : "Fluffy"
         |}
      """.trimMargin()
      val headers = HttpHeaders()
      headers.set(io.vyne.http.HttpHeaders.CONTENT_PREPARSED, true.toString())
      server.expect(ExpectedCount.once(), requestTo("http://pets.com/pets"))
         .andExpect(method(HttpMethod.GET))
         .andRespond(MockRestResponseCreators
            .withSuccess(responseJson, MediaType.APPLICATION_JSON)
            .headers(headers)
         )

      // Note: The jsonPaths are supposed to ignored, because the content is preparsed
      val schema = TaxiSchema.from("""
         service PetService {
            @HttpOperation(method = "GET",url = "http://pets.com/pets")
            operation getBestPet():Animal
         }
         model Animal {
            id : String by jsonPath("$.animalsId")
            name : String by jsonPath("$.animalName")
         }
      """)

      val schemaProvider = SchemaProvider.from(schema)
      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")

      val response = RestTemplateInvoker(
         webClient = webClient,
         serviceUrlResolvers = listOf(AbsoluteUrlResolver()),
         enableDataLineageForRemoteCalls = true,
         schemaProvider = schemaProvider)
         .invoke(service, operation, emptyList(), QueryProfiler()) as TypedObject

      response["id"].value.should.equal("100")
      response["name"].value.should.equal("Fluffy")
   }

   @Test
   fun `when invoking a service without preparsed content then accessors are not evaluated`() {
      val restTemplate = RestTemplate()
      val webClient = WebClient.builder().build()
      val server = MockRestServiceServer.bindTo(restTemplate).build()
      val responseJson = """{
         |"animalsId" : 100,
         |"animalName" : "Fluffy"
         |}
      """.trimMargin()
      server.expect(ExpectedCount.once(), requestTo("http://pets.com/pets"))
         .andExpect(method(HttpMethod.GET))
         .andRespond(MockRestResponseCreators
            .withSuccess(responseJson, MediaType.APPLICATION_JSON)
         )

      // Note: The jsonPaths are supposed to ignored, because the content is preparsed
      val schema = TaxiSchema.from("""
         service PetService {
            @HttpOperation(method = "GET",url = "http://pets.com/pets")
            operation getBestPet():Animal
         }
         model Animal {
            id : String by jsonPath("$.animalsId")
            name : String by jsonPath("$.animalName")
         }
      """)

      val schemaProvider = SchemaProvider.from(schema)
      val service = schema.service("PetService")
      val operation = service.operation("getBestPet")

      val response = RestTemplateInvoker(
         webClient = webClient,
         serviceUrlResolvers = listOf(AbsoluteUrlResolver()),
         enableDataLineageForRemoteCalls = true,
         schemaProvider = schemaProvider)
         .invoke(service, operation, emptyList(), QueryProfiler()) as TypedObject

      response["id"].value.should.equal("100")
      response["name"].value.should.equal("Fluffy")
   }
}
