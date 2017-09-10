package io.osmosis.polymer.demos

import com.winterbe.expekt.expect
import io.osmosis.polymer.Polymer
import io.osmosis.polymer.StubService
import io.osmosis.polymer.models.json.parseJsonModel
import io.osmosis.polymer.query.QueryEngineFactory
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
        operation toSic2003( isic.uk.SIC2003 ) : isic.uk.SIC2003
        @StubResponse
        operation toSic2007( isic.uk.SIC2003 ) : isic.uk.SIC2008
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
      // Set up stub service responses
      stubService.addResponse("findClientById", polymer.parseJsonModel("polymer.creditInc.Client", clientJson))
      stubService.addResponse("calculateCreditCosts", polymer.parseJsonModel("polymer.creditInc.CreditCostResponse", creditCostResponse))

      val invoice = polymer.parseJsonModel("polymer.creditInc.Invoice", invoiceJson)
      val result = polymer.query().find("polymer.creditInc.CreditRiskCost", setOf(invoice))
      expect(result["polymer.creditInc.CreditRiskCost"]!!.value).to.equal(250.0)
   }
}
