package io.vyne.demos

import com.winterbe.expekt.expect
import io.vyne.Vyne
import io.vyne.StubService
import io.vyne.models.json.parseJsonModel
import io.vyne.query.QueryEngineFactory
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class InvoiceMarkupTest {
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
        operation convertRates( vyne.creditInc.Money, vyne.creditInc.Currency ) : vyne.creditInc.Money
    }
}

namespace vyne.creditInc {
    parameter type CreditCostRequest {
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
      // Set up stub service responses
      stubService.addResponse("findClientById", vyne.parseJsonModel("vyne.creditInc.Client", clientJson))
      stubService.addResponse("calculateCreditCosts", vyne.parseJsonModel("vyne.creditInc.CreditCostResponse", creditCostResponse))

      val invoice = vyne.parseJsonModel("vyne.creditInc.Invoice", invoiceJson)
      val result = vyne.query().find("vyne.creditInc.CreditRiskCost", setOf(invoice))
      expect(result["vyne.creditInc.CreditRiskCost"]!!.value).to.equal(250.0)
   }
}
