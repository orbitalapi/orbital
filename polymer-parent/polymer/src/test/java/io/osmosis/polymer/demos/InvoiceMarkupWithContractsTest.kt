package io.osmosis.polymer.demos

import com.winterbe.expekt.expect
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.StubService
import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.models.TypedObject
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.models.json.parseKeyValuePair
import io.osmosis.polymer.query.QueryEngineFactory
import io.osmosis.polymer.query.TypeName
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

class InvoiceMarkupWithContractsTest {
   val taxiDef = """
namespace polymer.creditInc {
    type Client {
        clientId : ClientId
    }
    type alias ClientId as String
     type Invoice {
        clientId : ClientId
        settlementDate : settlementDate
        amount : Money
    }

    type alias settlementDate as Date
}

namespace polymer.creditInc {
    type Client {
        clientId : ClientId
        clientName : ClientName
        sicCode : isic.uk.SIC2008
    }

    type alias ClientId as String
    type alias ClientName as String
}

namespace io.osmosis.demos.creditInc.clientLookup {
    service ClientLookupService {
        @StubResponse
        operation findClientById( polymer.creditInc.ClientId ) : polymer.creditInc.Client
    }
}

namespace polymer.creditInc {
    type Money {
        currency : Currency
        amount : MoneyAmount
    }

    type alias Currency as String
    type alias MoneyAmount as Decimal
}

namespace io.osmosis.demos.invictus.rates {
    service RateConversionService {
        @StubResponse
        operation convertRates( polymer.creditInc.Money, polymer.creditInc.Currency ) : polymer.creditInc.Money( from source, currency = targetCurrency )
    }
}

namespace polymer.creditInc {
    parameter type CreditCostRequest {
        invoiceValue : Money(currency = 'GBP')
        industryCode : isic.uk.SIC2003
    }
     type Money {
        currency : Currency
        value : MoneyAmount
    }
    type alias Currency as String
    type alias MoneyAmount as Decimal
     type CreditCostResponse {
        cost : CreditRiskCost
    }
    type alias CreditRiskCost as Decimal
}

namespace polymer.creditInc.creditMarkup {
    service CreditCostService {
        @StubResponse
        operation calculateCreditCosts( polymer.creditInc.CreditCostRequest ) : polymer.creditInc.CreditCostResponse
    }
}

namespace isic.uk {
    type alias SIC2003 as String
    type alias SIC2008 as String
}

namespace io.osmosis.demos.creditInc.isic {
    service IsicConversionService {
        @StubResponse
        operation toSic2003( isic.uk.SIC2008 ) : isic.uk.SIC2003
        @StubResponse
        operation toSic2008( isic.uk.SIC2003 ) : isic.uk.SIC2008
    }
}
       """
   val schema = TaxiSchema.from(taxiDef)

   // Responses:

   val clientJson = """
{
    "clientId": "jim01",
    "clientName": "Jim's Bar & Grill",
    "sicCode": "2008-123456"
}"""

   @Test
   fun runTest() {
      val stubService = StubService()
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(stubService)
      val polymer = Polymer(queryEngineFactory).addSchema(schema)

      val invoiceJson = """
{
	"clientId" : "jim01",
	"settlementDate" : "2017-10-20",
	"amount" : {
		"currency" : "AUD",
		"value" : "20.55"
	}
}
"""

      val creditCostResponse = """
{
"cost" : 250.00
}"""

      val rateConversionResponse = """
{
   "currency" : "GBP",
   "value" : "10.00"
}
"""
      // Set up stub service responses
      stubService.addResponse("findClientById", polymer.parseJsonModel("polymer.creditInc.Client", clientJson))
      stubService.addResponse("calculateCreditCosts", polymer.parseJsonModel("polymer.creditInc.CreditCostResponse", creditCostResponse))
      stubService.addResponse("toSic2003", polymer.parseKeyValuePair("isic.uk.SIC2003", "2003"))
      stubService.addResponse("convertRates", polymer.parseJsonModel("polymer.creditInc.Money", rateConversionResponse))

      val invoice = polymer.parseJsonModel("polymer.creditInc.Invoice", invoiceJson)
      val result = polymer.query().find("polymer.creditInc.CreditRiskCost", setOf(invoice))
// This is the expected (raw) solution -- other searches exist within this path:
//      Search Type_instance(polymer.creditInc.Invoice) -> Type(polymer.creditInc.CreditRiskCost) found path:
//      polymer.creditInc.Invoice -[Instance has attribute]-> polymer.creditInc.Invoice/amount
//      polymer.creditInc.Invoice/amount -[Is an attribute of]-> polymer.creditInc.Money
//      polymer.creditInc.Money -[can populate]-> param/polymer.creditInc.Money
//      param/polymer.creditInc.Money -[Is parameter on]-> param/polymer.creditInc.CreditCostRequest
//      param/polymer.creditInc.CreditCostRequest -[Is parameter on]-> polymer.creditInc.creditMarkup.CreditCostService@@calculateCreditCosts
//      polymer.creditInc.creditMarkup.CreditCostService@@calculateCreditCosts -[provides]-> polymer.creditInc.CreditCostResponse
//      polymer.creditInc.CreditCostResponse -[Is instance of]-> polymer.creditInc.CreditCostResponse
//      polymer.creditInc.CreditCostResponse -[Has attribute]-> polymer.creditInc.CreditCostResponse/cost
//      polymer.creditInc.CreditCostResponse/cost -[Is type of]-> polymer.creditInc.CreditRiskCost

      expect(result["polymer.creditInc.CreditRiskCost"]!!.value).to.equal(250.0)

      // Validate the services were called correctly
      expect(stubService.invocations["findClientById"]!!).to.satisfy { containsArg(it, "polymer.creditInc.ClientId", "jim01") }
      expect(stubService.invocations["toSic2003"]!!).to.satisfy { containsArg(it, "isic.uk.SIC2008", "2008-123456") }
      expect(stubService.invocations["convertRates"]!!).to.satisfy {
         containsArgWithParams(it, "polymer.creditInc.Money",
            Triple("currency", "polymer.creditInc.Currency", "AUD"),
            Triple("value", "polymer.creditInc.MoneyAmount","20.55")
         )
      }
      expect(stubService.invocations["convertRates"]!!).to.satisfy { containsArg(it, "polymer.creditInc.Currency", "GBP" ) }
      val creditRiskRequest = stubService.invocations["calculateCreditCosts"]!!.first() as TypedObject
      // Assert we called with the converted currency
      expect(creditRiskRequest["invoiceValue.currency"].value).to.equal("GBP")
      expect(creditRiskRequest["invoiceValue.value"].value).to.equal("10.00")
      // Assert we called with the converted SIC code.
      expect(creditRiskRequest["industryCode"].value).to.equal("2003")
//      expect(stubService.invocations["calculateCreditCosts"]).to.satisfy { containsArgWithParams(it, "polymer.creditInc.CreditCostRequest",
//         Triple("invoiceValue", )
//         ) }
   }
}

typealias ParamName = String
fun containsArgWithParams(args: Collection<TypedInstance>, type: String, vararg params: Triple<ParamName, TypeName, Any>): Boolean {
   return args.any {
      it.type.fullyQualifiedName == type &&
         params.all { (paramName, typeName, expectedValue) -> (it as TypedObject)[paramName].value == expectedValue && it[paramName].type.fullyQualifiedName == typeName }
   }
}

fun containsArg(args: Collection<TypedInstance>, type: String, value: Any): Boolean {
   return args.any { it.type.fullyQualifiedName == type && it.value == value }
}
