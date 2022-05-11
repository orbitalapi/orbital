package io.vyne.demos

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import io.vyne.StubService
import io.vyne.Vyne
import io.vyne.VyneCacheConfiguration
import io.vyne.firstTypedInstace
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.json.parseJsonModel
import io.vyne.models.json.parseKeyValuePair
import io.vyne.query.QueryEngineFactory
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal

class InvoiceMarkupWithContractsTest {
   val taxiDef = """
namespace vyne.creditInc {
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

namespace vyne.creditInc {
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
        operation findClientById( vyne.creditInc.ClientId ) : vyne.creditInc.Client
    }
}

namespace vyne.creditInc {
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
        operation convertRates( vyne.creditInc.Money, targetCurrency : vyne.creditInc.Currency ) : vyne.creditInc.Money( from source, this.currency == targetCurrency )
    }
}

namespace vyne.creditInc {
    parameter type CreditCostRequest {
        invoiceValue : Money(this.currency == 'GBP')
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

namespace vyne.creditInc.creditMarkup {
    service CreditCostService {
        @StubResponse
        operation calculateCreditCosts( vyne.creditInc.CreditCostRequest ) : vyne.creditInc.CreditCostResponse
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
      val queryEngineFactory = QueryEngineFactory.withOperationInvokers(VyneCacheConfiguration.default(),  emptyList(), emptyList(), stubService)
      val vyne = Vyne(queryEngineFactory).addSchema(schema)

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
      stubService.addResponse("findClientById", vyne.parseJsonModel("vyne.creditInc.Client", clientJson))
      stubService.addResponse("calculateCreditCosts", vyne.parseJsonModel("vyne.creditInc.CreditCostResponse", creditCostResponse))
      stubService.addResponse("toSic2003", vyne.parseKeyValuePair("isic.uk.SIC2003", "2003"))
      stubService.addResponse("convertRates", vyne.parseJsonModel("vyne.creditInc.Money", rateConversionResponse))

      val invoice = vyne.parseJsonModel("vyne.creditInc.Invoice", invoiceJson)
      val result = runBlocking {vyne.query(additionalFacts = setOf(invoice)).find("vyne.creditInc.CreditRiskCost")}
// This is the expected (raw) solution -- other searches exist within this path:
//      Search Type_instance(vyne.creditInc.Invoice) -> Type(vyne.creditInc.CreditRiskCost) found path:
//      vyne.creditInc.Invoice -[Instance has attribute]-> vyne.creditInc.Invoice/amount
//      vyne.creditInc.Invoice/amount -[Is an attribute of]-> vyne.creditInc.Money
//      vyne.creditInc.Money -[can populate]-> param/vyne.creditInc.Money
//      param/vyne.creditInc.Money -[Is parameter on]-> param/vyne.creditInc.CreditCostRequest
//      param/vyne.creditInc.CreditCostRequest -[Is parameter on]-> vyne.creditInc.creditMarkup.CreditCostService@@calculateCreditCosts
//      vyne.creditInc.creditMarkup.CreditCostService@@calculateCreditCosts -[provides]-> vyne.creditInc.CreditCostResponse
//      vyne.creditInc.CreditCostResponse -[Is instance of]-> vyne.creditInc.CreditCostResponse
//      vyne.creditInc.CreditCostResponse -[Has attribute]-> vyne.creditInc.CreditCostResponse/cost
//      vyne.creditInc.CreditCostResponse/cost -[Is type of]-> vyne.creditInc.CreditRiskCost
      val operation = result.profilerOperation
      log().debug(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(operation))


   runBlocking {
      expect(result.firstTypedInstace()!!.value).to.equal(250.0.toBigDecimal())
   }

      // Validate the services were called correctly
      expect(stubService.invocations["findClientById"]!!).to.satisfy { containsArg(it, "vyne.creditInc.ClientId", "jim01") }
      expect(stubService.invocations["toSic2003"]!!).to.satisfy { containsArg(it, "isic.uk.SIC2008", "2008-123456") }
      expect(stubService.invocations["convertRates"]!!).to.satisfy {
         containsArgWithParams(it, "vyne.creditInc.Money",
            Triple("currency", "vyne.creditInc.Currency", "AUD"),
            Triple("value", "vyne.creditInc.MoneyAmount", 20.55.toBigDecimal())
         )
      }
      expect(stubService.invocations["convertRates"]!!).to.satisfy { containsArg(it, "vyne.creditInc.Currency", "GBP") }
      val creditRiskRequest = stubService.invocations["calculateCreditCosts"]!!.first() as TypedObject
      // Assert we called with the converted currency
      expect(creditRiskRequest["invoiceValue.currency"].value).to.equal("GBP")
      expect(creditRiskRequest["invoiceValue.value"].value).to.equal(BigDecimal("10.00"))
      // Assert we called with the converted SIC code.
      expect(creditRiskRequest["industryCode"].value).to.equal("2003")
//      expect(stubService.invocations["calculateCreditCosts"]).to.satisfy { containsArgWithParams(it, "vyne.creditInc.CreditCostRequest",
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

typealias TypeName = String

