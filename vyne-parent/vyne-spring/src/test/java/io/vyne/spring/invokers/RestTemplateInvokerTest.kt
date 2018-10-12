package io.vyne.spring.invokers

import com.jayway.jsonpath.JsonPath
import com.winterbe.expekt.expect
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.query.QueryProfiler
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.schemaStore.SchemaProvider
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
namespace vyne {

    type CreditCostRequest {
        deets : String
    }

    type alias ClientId as String

     type CreditCostResponse {
        stuff : String
    }


    @ServiceDiscoveryClient(serviceName = "mockService")
    service CreditCostService {
        @HttpOperation(method = "POST",url = "/costs/{vyne.ClientId}/doCalculate")
        Operation calculateCreditCosts(@RequestBody CreditCostRequest, ClientId ) : CreditCostResponse
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
      val service = schema.service("vyne.CreditCostService")
      val operation = service.operation("calculateCreditCosts")

      val response = RestTemplateInvoker(restTemplate = restTemplate, schemaProvider = SchemaProvider.from(schema)).invoke(service,operation, listOf(
         TypedInstance.from(schema.type("vyne.ClientId"), "myClientId", schema),
         TypedObject.fromAttributes("vyne.CreditCostRequest", mapOf("deets" to "Hello, world"), schema)
      ), QueryProfiler()) as TypedObject
      expect(response.type.fullyQualifiedName).to.equal("vyne.CreditCostResponse")
      expect(response["stuff"].value).to.equal("Right back atcha, kid")
   }
}
