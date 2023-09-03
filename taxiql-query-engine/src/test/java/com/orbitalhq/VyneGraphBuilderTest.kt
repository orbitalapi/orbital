package com.orbitalhq

import es.usc.citius.hipster.graph.GraphEdge
import io.kotest.matchers.booleans.shouldBeFalse
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.graph.*
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.Relationship
import com.orbitalhq.schemas.Relationship.REQUIRES_PARAMETER
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.types.PrimitiveType
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

      val taxiSchema = TaxiSchema.from(taxiDef)
      val (service, operation) = taxiSchema.operation(
         OperationNames.name("CustomerService", "getCustomerByEmail").fqn()
      )
      val graph =
         VyneGraphBuilder(taxiSchema, VyneGraphBuilderCacheSettings(100L, 100L, 100L)).buildDisplayGraph()
      val edges = graph.outgoingEdgesOf(operation(service, operation))

      edges.shouldContain(REQUIRES_PARAMETER, type("CustomerEmailAddress"))

   }

   @Test
   fun `should not generate paths to services accepting primitives as input`() {
      val schema = TaxiSchema.from(
         """
            | // Schemas like this can be produced from code-gen tasks,
            |  // where everything is not semantically typed.
            |model Person {
            |   id : String
            |}
            |model PersonDetails {
            |   age : Int
            |}
            |
            |service PeopleService {
            |  operation getDetails(personId: String):PersonDetails
            |}
         """.trimMargin()
      )
      val graph =
         VyneGraphBuilder(schema, VyneGraphBuilderCacheSettings(100L, 100L, 100L))
            .buildDisplayGraph()

      graph.vertices().any {
         it.elementType == ElementType.OPERATION && it.value == OperationNames.name(
            "PeopleService",
            "getDetails"
         )
      }
         .shouldBeFalse()

      val startFact = TypedInstance.from(schema.type(PrimitiveType.STRING.qualifiedName), "jimmy", schema)

      val graphWithStartFact =
         VyneGraphBuilder(schema, VyneGraphBuilderCacheSettings(100L, 100L, 100L))
            .build(facts = listOf(startFact), excludedEdges = emptyList(), excludedServices = emptySet())
      graphWithStartFact.graph.vertices().any {
         it.elementType == ElementType.OPERATION && it.value == OperationNames.name(
            "PeopleService",
            "getDetails"
         )
      }
         .shouldBeFalse()
   }

//   @Test
//   fun performanceTestBuildingLargeGraph() {
//      val schema = TaxiSchema.forPackageAtPath(Paths.get("/home/marty/dev/cacib/cacib-taxonomy"))
//
//      val fact = TypedInstance.from(
//         schema.type("bgc.orders.Order"),
//         """{"bgcOrderID":"33438611","entryType":"New","tradeNo":null,"orderDateTime":"2020-10-21T01:58:55.406Z","assetClass":"IRD","identifierType":"ISIN","identiferValue":"","isin":null,"securityDescription":"LCH-USD SB/3ML : 5Y","strategy":null,"cfiCode":"SRCCSP","priceAmount":0.4275,"strikePrice":null,"priceType":"percentage","tradingSystem":"bgcRates","quantityType":"MONE","tmpMultiplier":1000000.0,"tmpRequestedQuantity":75.0,"tmpCumulativeQuantity":0.0,"tmpExecutedQuantity":0.0,"tmpRemainingQuantity":75.0,"tmpDisplayedQuantity":null,"requestedQuantity":7.5E+7,"cumulativeQuantity":0.0,"executedQuantity":0.0,"remainingQuantity":7.5E+7,"tmp2DisplayedQuantity":75.0,"displayedQuantity":7.5E+7,"unitMultiplier":1.0,"quantityCurrency":null,"orderType":"Limit","buySellIndicator":"buy","orderValidityPeriod":"Good Till Cancel","exchange":"BGCD","broker":"BGCD","venueOrderStatus":"New","cacibTraderBrokerLogin":"charles.folan@bgcpartners.com","brokerLEI":null,"orderMethod":"ELECTRONIC","underlyingIndexName":"USD-LIBOR-BBA","maturityDate":null,"activityCategory":"Hedge","leg1NotionalValue":7.5E+7,"leg1OrigCurrNotionalAmount":7.5E+7,"leg2OrigCurrNotionalAmount":7.5E+7,"leg1NotionalCurrencyCd":"","leg2NotionalCurrencyCd":"","leg2NotionalValue":7.5E+7,"tempPayReceive":"SRCCSP-buy","leg1PayReceive":"Pay","leg2PayReceive":"Receive","tempLegRate":"SRCCSP-buy","leg1Rate":0.4275,"leg2Rate":null,"leg1DayCountMethodInd":null,"leg2DayCountMethodInd":null,"leg1PaymentFrequency":null,"leg2PaymentFrequency":null,"clientid":"SC0000198746","counterpartyLei":"549300P5P8RXOD23W720","counterParty":"BGC EUROPEAN HOLDINGS LP","cacibLei":"1VUV7VQFKUOQSJ21A208"}""",
//         schema,
//         source = Provided,
//         evaluateAccessors = false
//      )
//      val builder = VyneGraphBuilder(schema, VyneGraphBuilderCacheSettings())
//      Benchmark.benchmark("build a graph", warmup = 100, iterations = 500) {
//         builder.build(listOf(fact), excludedServices = emptySet(), excludedEdges = emptyList())
//      }
//   }
}

private fun Iterable<GraphEdge<Element, Relationship>>.shouldContain(relationship: Relationship, element: Element) {
   require(this.any {
      it.edgeValue == relationship && it.vertex2 == element
   }) {
      "Expected to find relationship $relationship -> $element"
   }

}