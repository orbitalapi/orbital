package io.vyne.query.graph

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.VyneCacheConfiguration
import io.vyne.expectTypedObject
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.query.QueryEngineFactory
import io.vyne.query.graph.operationInvocation.CacheAwareOperationInvocationDecorator
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class ParameterTypeTest {
   private val taxiDef = """
namespace vyne.example
      type EmployeeId inherits String
      type SocialSecurityId inherits String
      type Title inherits String
      type DepartmentName inherits String


      model Employee {
         id : EmployeeId
         socialSecurityId: SocialSecurityId?
      }

      model EmployeeSocialSecurity {
        socialSecurityId: SocialSecurityId
        startDate: StartDate as Date
      }

      parameter model EmployeeDetailsRequest {
         socialSecurityId: SocialSecurityId
      }

      model EmployeeDetails {
        title: Title
      }


      service EmployeeService {
         @StubResponse("mockEmployeeSocialSecurity")
         operation getEmployeeSocialSecurity(EmployeeId):EmployeeSocialSecurity

         @StubResponse("mockEmployeeDetails")
         operation getEmployeeDetails(EmployeeDetailsRequest):EmployeeDetails

         @StubResponse("mockEmployees")
         operation getEmployees():Employee[]
      }
"""
   private val schema = TaxiSchema.from(taxiDef)
   private val stubService = StubService(schema = schema)
   private val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(), CacheAwareOperationInvocationDecorator(stubService))
   private val vyne = Vyne(listOf(schema), queryEngineFactory)

   /***
    * This test ensures that starting from:
    * {
    *    "id": "1" // employee id
    *    "socialSecurityId": null
    * }
    *
    * Vyne will  able to derive 'employee title' using this path:
    *
    * Start : Type_instance(vyne.example.Employee@-718754891)
    *  vyne.example.Employee@-718754891 -[Instance has attribute]-> vyne.example.Employee/id (cost: 1.0)
    *  vyne.example.Employee/id -[Is an attribute of]-> vyne.example.EmployeeId (cost: 2.0)
    *  vyne.example.EmployeeId -[can populate]-> param/vyne.example.EmployeeId (cost: 3.0)
    *  param/vyne.example.EmployeeId -[Is parameter on]-> example/EmployeeService@@getEmployeeSocialSecurity (cost: 4.0)
    *  example/EmployeeService@@getEmployeeSocialSecurity -[provides]-> vyne.example.EmployeeSocialSecurity (cost: 5.0)
    *  vyne.example.EmployeeSocialSecurity -[Instance has attribute]-> vyne.example.EmployeeSocialSecurity/socialSecurityId (cost: 6.0)
    *  vyne.example.EmployeeSocialSecurity/socialSecurityId -[Is an attribute of]-> vyne.example.SocialSecurityId (cost: 7.0)
    *  vyne.example.SocialSecurityId -[can populate]-> param/vyne.example.SocialSecurityId (cost: 8.0)
    *  param/vyne.example.SocialSecurityId -[Is parameter on]-> param/vyne.example.EmployeeDetailsRequest (cost: 9.0)
    *  param/vyne.example.EmployeeDetailsRequest -[Is parameter on]-> example/EmployeeService@@getEmployeeDetails (cost: 10.0)
    *  example/EmployeeService@@getEmployeeDetails -[provides]-> vyne.example.EmployeeDetails (cost: 11.0)
    *  vyne.example.EmployeeDetails -[Instance has attribute]-> vyne.example.EmployeeDetails/title (cost: 12.0)
    *  vyne.example.EmployeeDetails/title -[Is an attribute of]-> vyne.example.Title (cost: 13.0)
    *  vyne.example.Title -[Is instanceOfType of]-> Title (cost: 14.0)
    *
    * The important step is the population of EmployeeDetailsRequest which is a 'parameter' model. In the context, we have:
    * {
    *    "id": "1" // employee id
    *    "socialSecurityId": null
    * }
    *
    * so in ParameterFactory::attemptToConstruct the following line will yield a 'TypedNull'
    *
    * var attributeValue: TypedInstance? =
    * context.getFactOrNull(attributeType, FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT)
    *
    * We need to make sure that the next attempt in 'ParameterFactory::attemptToConstruct' must use the 'previousValue' yielded by:
    * example/EmployeeService@@getEmployeeSocialSecurity -[provides]-> vyne.example.EmployeeSocialSecurity
    * step.
    *
    */
   @ExperimentalTime
   @Test
   fun `Can fetch details from id`() {
      runBlocking {
         val employees = """[{ "id" :"1", "socialSecurityId": null }]"""
         val employeeSocialSecurity = """
         {
            "socialSecurityId": "XYZ",
            "startDate": "1990-01-01"
         }
      """.trimIndent()
         val employeeDetails = """
         {
           "title": "Director"
         }
      """.trimIndent()
         stubService.addResponse("mockEmployees", TypedInstance.from(vyne.type("vyne.example.Employee[]"), employees, vyne.schema, source = Provided))
         stubService.addResponse("mockEmployeeSocialSecurity", TypedInstance.from(vyne.type("vyne.example.EmployeeSocialSecurity"), employeeSocialSecurity, vyne.schema, source = Provided))
         stubService.addResponse("mockEmployeeDetails", TypedInstance.from(vyne.type("vyne.example.EmployeeDetails"), employeeDetails, vyne.schema, source = Provided))
         // act
         val result =
            vyne.query("""findAll { Employee[] } as EmployeeDetails[]""".trimIndent())

         result.results.test(Duration.INFINITE) {
            expectTypedObject()["title"].value.should.equal("Director")
            expectComplete()
         }

      }
   }
}
