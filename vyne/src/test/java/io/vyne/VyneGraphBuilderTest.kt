package io.vyne

import es.usc.citius.hipster.graph.GraphEdge
import io.vyne.query.graph.Element
import io.vyne.query.graph.VyneGraphBuilder
import io.vyne.query.graph.operation
import io.vyne.query.graph.type
import io.vyne.schemas.OperationNames
import io.vyne.schemas.Relationship
import io.vyne.schemas.Relationship.REQUIRES_PARAMETER
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
      val graph = VyneGraphBuilder(TaxiSchema.from(taxiDef), VyneGraphBuilderCacheSettings(100L, 100L, 100L)).buildDisplayGraph()
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
