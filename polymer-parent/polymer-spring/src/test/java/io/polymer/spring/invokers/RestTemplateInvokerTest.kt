package io.polymer.spring.invokers

import com.jayway.jsonpath.JsonPath
import com.winterbe.expekt.expect
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import io.polymer.schemaStore.SchemaProvider
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate

class RestTemplateInvokerTest {
   val taxiDef = """
namespace polymer {

    type CreditCostRequest {
        deets : String
    }

    type alias ClientId as String

     type CreditCostResponse {
        stuff : String
    }


    @ServiceDiscoveryClient(serviceName = "mockService")
    service CreditCostService {
        @HttpOperation(method = "POST",url = "/costs/{polymer.ClientId}/doCalculate")
        operation calculateCreditCosts(@RequestBody CreditCostRequest, ClientId ) : CreditCostResponse
    }
}      """

   fun isJsonSatisfying(jsonPath:String, expectedValue:Any):Matcher<String> {
      return object : BaseMatcher<String>() {
         private var actualBody:String? = null
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
   fun when_invokingService_then_itGetsInvokedCorrectly() {
      val restTemplate = RestTemplate()
      val server = MockRestServiceServer.bindTo(restTemplate).build()

      server.expect(ExpectedCount.once(), requestTo("http://mockService/costs/myClientId/doCalculate"))
         .andExpect(method(HttpMethod.POST))
         .andExpect(content().string(isJsonSatisfying("$.deets","Hello, world")))
         .andRespond(MockRestResponseCreators.withSuccess("""{ "stuff" : "Right back atcha, kid" }""", MediaType.APPLICATION_JSON))
      val schema = TaxiSchema.from(taxiDef)
      val service = schema.service("polymer.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      val response = RestTemplateInvoker(restTemplate = restTemplate, schemaProvider = SchemaProvider.from(schema)).invoke(service,operation, listOf(
         TypedInstance.from(schema.type("polymer.ClientId"), "myClientId", schema),
         TypedObject.fromAttributes("polymer.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
      )) as TypedObject
      expect(response.type.fullyQualifiedName).to.equal("polymer.CreditCostResponse")
      expect(response["stuff"].value).to.equal("Right back atcha, kid")
   }
}
