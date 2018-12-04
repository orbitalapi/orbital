package io.vyne

import es.usc.citius.hipster.graph.GraphEdge
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
import io.vyne.schemas.Relationship.*
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class VyneGraphBuilderTest {
   val schema = TaxiSchema.from("""
 namespace vyne {
    parameter type JurisdictionRuleRequest {
        clientJurisdiction : vyne.ClientJurisdiction
    }
    service ClientService {
        operation getClient(  vyne.ClientId ) : Client
    }
    service JurisdictionRuleService {
        operation evaluate( JurisdictionRuleRequest ) : RuleEvaluationResult
    }
    type Client {
        id : vyne.ClientId as String
        name : String
        jurisdiction : vyne.ClientJurisdiction as String
    }
    type RuleEvaluationResult {
        message : String
    }
 }
   """.trimIndent())

   @Test
   fun typeInstance_canPopulate_otherObjects() {
      val graph = VyneGraphBuilder(schema).build()
      graph.outgoingEdgesOf(operation("vyne.ClientService@@getClient")).shouldContain(PROVIDES, providedInstance("vyne.Client"))
      graph.outgoingEdgesOf(providedInstance("vyne.Client")).shouldContain(INSTANCE_HAS_ATTRIBUTE, providedInstanceMember("vyne.Client/jurisdiction"))


      TODO()
   }

   @Test
   fun generatesParamsCorrectly() {
      val taxiDef = """
    type Customer {
      email : CustomerEmailAddress as String
      id : CustomerId as Int
      name : CustomerName as String
   }
   service CustomerService {
      operation getCustomerByEmail(  CustomerEmailAddress ) : Customer
   }
      """.trimIndent()
      val graph = VyneGraphBuilder(TaxiSchema.from(taxiDef)).buildDisplayGraph()
      val edges = graph.outgoingEdgesOf(operation(OperationNames.displayName("CustomerService", "getCustomerByEmail")))
      edges.shouldContain(REQUIRES_PARAMETER, type("CustomerEmailAddress"))

   }
}

private fun Iterable<GraphEdge<Element, Relationship>>.shouldContain(relationship: Relationship, element: Element) {
   require(this.any {
      it.edgeValue == relationship && it.vertex2 == element
   }) {
      "Expected to find relationship $relationship -> $element"
   }

}
