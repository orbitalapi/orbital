package com.orbitalhq.regression

import com.orbitalhq.firstTypedObject
import com.orbitalhq.testVyne
import io.kotest.core.spec.style.DescribeSpec

class ComplexExpressionsSpec : DescribeSpec({
   describe("Complex expressions") {
      it("should work") {
         val schema = """
// Instrument identification and classification types
[[
  International Securities Identification Number - a unique 12-character alphanumeric code
  that identifies a specific security for trading and settlement purposes.
  Format: Two-letter country code + nine-character identifier + check digit
  Example: "US0378331005" for Apple Inc.
]]
type IsinCode inherits String

[[
  Classification of financial instrument according to MiFID II categories.
  Examples: "Equity", "Bond", "Derivative", "Commodity", "Currency"
  Used to determine applicable trading rules and transparency requirements.
]]
type AssetClass inherits String

// Trading participant identification types
[[
  Unique identifier for an authorized trading representative.
  Must be registered with relevant competent authority.
]]
type TraderId inherits String

[[
  ISO 3166-1 alpha-2 country code representing regulatory jurisdiction.
  Examples: "GB" (United Kingdom), "DE" (Germany), "FR" (France)
  Determines applicable MiFID II national implementation rules.
]]
type Country inherits String

[[
  Trading role classification defining permitted activities.
  Examples: "Dealer", "Broker", "Market Maker", "Proprietary Trader"
  Affects position limits and reporting obligations under MiFID II.
]]
type Role inherits String

// Counterparty identification and classification
[[
  Legal Entity Identifier (LEI) or other unique party identifier.
  20-character alphanumeric code for legal entities participating in financial transactions.
]]
type PartyId inherits String

[[
  MiFID II client classification determining protection level and trading restrictions.
  Values: "Retail Client", "Professional Client", "Eligible Counterparty"
  Affects conduct of business rules and best execution requirements.
]]
type CounterpartyClassification inherits String

// Venue identification
[[
  Market Identifier Code - ISO 10383 four-character code identifying trading venues.
  Examples: "XLON" (London Stock Exchange), "XPAR" (Euronext Paris)
  Used for transaction reporting and best execution analysis.
]]
type MicCode inherits String

// Compliance and regulatory status types
[[
  Human-readable explanation of regulatory check outcome.
  Must be sufficiently detailed for audit trail and regulatory reporting.
]]
type ReasonText inherits String

[[
  Indicates whether the financial instrument is available for trading.
  Must be true for any instrument to be eligible for execution under MiFID II.
  Checked against venue's official list of tradable instruments.
]]
type IsTradable inherits Boolean

[[
  Confirms the instrument is formally admitted to trading on a regulated market.
  Required under MiFID II Article 4(1)(21) for systematic internalisation rules.
  Must be verified against official market admission documentation.
]]
type IsAdmittedToTrading inherits Boolean

[[
  Indicates whether the execution venue is regulated under MiFID II.
  True for: Regulated Markets (RM), Multilateral Trading Facilities (MTF),
  Organised Trading Facilities (OTF). False for bilateral or unregulated venues.
]]
type IsRegulated inherits Boolean

[[
  Result of MiFID II compliance verification.
  True indicates all regulatory requirements are satisfied for trade execution.
  False requires trade rejection or escalation to compliance team.
]]
type CompliancePassed inherits Boolean



// Models for structured data
model Instrument {
  isin: IsinCode
  assetClass: AssetClass
  tradable: IsTradable
  admittedToTrading: IsAdmittedToTrading
}

type AuthorizedCountry inherits Country
model Trader {
  id: TraderId
  authorisedIn: AuthorizedCountry[]
  role: Role
}

type CounterpartyCountry inherits Country
model Counterparty {
  id: PartyId
  jurisdiction: CounterpartyCountry
  classification: CounterpartyClassification
}

type ExecutionVenueCountry inherits Country
model ExecutionVenue {
  micCode: MicCode
  location: ExecutionVenueCountry
  regulated: IsRegulated
}

model Trade {
  instrument: IsinCode
  trader: TraderId
  counterparty: PartyId
  executionVenue: MicCode
}

model RegulatoryCheckResult {
  passed: CompliancePassed
  reason: ReasonText
}

service InstrumentReferenceDataService {
   [[ Retrieve instrument details by ISIN for compliance checking ]]
   @HttpOperation(method = "GET", url = "/instruments/{isin}")
   operation getInstrument(
      @PathVariable("isin") isin: IsinCode
   ): Instrument

   [[ List all tradable instruments for a given asset class ]]
   @HttpOperation(method = "GET", url = "/instruments?assetClass={assetClass}")
   operation getInstrumentsByAssetClass(
      @QueryParam("assetClass") assetClass: AssetClass
   ): Instrument[]
}

service ExecutionVenueService {
   [[ Get venue details by Market Identifier Code ]]
   @HttpOperation(method = "GET", url = "/venues/{micCode}")
   operation getVenue(
      @PathVariable("micCode") micCode: MicCode
   ): ExecutionVenue

   [[ List all regulated venues in a specific country ]]
   @HttpOperation(method = "GET", url = "/venues/regulated?country={country}")
   operation getRegulatedVenues(
      @QueryParam("country") country: Country
   ): ExecutionVenue[]
}

// Trading participant and counterparty services
service TradingParticipantService {
   [[ Retrieve trader authorization and role information ]]
   @HttpOperation(method = "GET", url = "/traders/{traderId}")
   operation getTrader(
      @PathVariable("traderId") traderId: TraderId
   ): Trader

   [[ Validate trader authorization for specific country ]]
   @HttpOperation(method = "GET", url = "/traders/{traderId}/authorization/{country}")
   operation checkTraderAuthorization(
      @PathVariable("traderId") traderId: TraderId,
      @PathVariable("country") country: Country
   ): Boolean
}

service CounterpartyService {
   [[ Get counterparty classification and jurisdiction ]]
   @HttpOperation(method = "GET", url = "/counterparties/{partyId}")
   operation getCounterparty(
      @PathVariable("partyId") partyId: PartyId
   ): Counterparty
}

// Trade processing and compliance services
service TradeService {
   [[ Retrieve trade details for compliance checking ]]
   @HttpOperation(method = "GET", url = "/trades/{tradeId}")
   operation getTrade(
      @PathVariable("tradeId") tradeId: String
   ): Trade

   [[ Submit new trade for processing ]]
   @HttpOperation(method = "POST", url = "/trades")
   write operation submitTrade(
      @HttpRequestBody trade: Trade
   ): Trade
}

service ComplianceService {
   [[ Perform MiFID II compliance check on trade ]]
   @HttpOperation(method = "POST", url = "/compliance/mifid2/check")
   operation checkMiFID2Compliance(
      @HttpRequestBody trade: Trade
   ): RegulatoryCheckResult
}

// Business logic as an expression type
[[
  MiFID II pre-trade compliance check ensuring all regulatory requirements are met.

  Validates:
  1. Instrument tradability and market admission status
  2. Execution venue regulatory authorization
  3. Trader jurisdiction authorization alignment with venue location

  Based on MiFID II Articles 4, 23, and 27 regarding systematic internalisation,
  best execution, and authorisation requirements.
]]
type CanExecuteUnderMiFIDII inherits RegulatoryCheckResult =
  when {
     IsTradable == true &&
     IsAdmittedToTrading == true &&
     IsRegulated == true &&
     AuthorizedCountry[].contains(ExecutionVenueCountry) -> {
           passed: true,
           reason: "All MiFID II conditions met: instrument tradable and admitted, venue regulated, trader authorized in jurisdiction"
        }
     else -> {
           passed: false,
           reason: "MiFID II compliance failure: check instrument status, venue regulation, and trader authorization"
        }
  }
         """.trimIndent()
         val (vyne,stub) = testVyne(schema)
         stub.addResponse("getInstrument","""
                    {
                        "isin": "FR0000120271",
                        "assetClass": "Equity",
                        "tradable": true,
                        "admittedToTrading": true
                    }""")
         stub.addResponse("getTrader",
            """
                    {
                        "id": "TRD001",
                        "authorisedIn": ["GB", "FR", "DE"],
                        "role": "Dealer"
                    }
                """
         )
         stub.addResponse("getCounterparty",
            """
                    {
                        "id": "LEI549300KLMNOPQRST02",
                        "jurisdiction": "FR",
                        "classification": "Professional Client"
                    }
                """
         )
         // Paris exchange
         stub.addResponse("getVenue",
            """
                    {
                        "micCode": "XPAR",
                        "location": "FR",
                        "regulated": true
                    }
                """
         )
        val result = vyne.query("""given {
    trade: Trade = {
        instrument: "FR0000120271",
        trader: "TRD001",
        counterparty: "LEI549300KLMNOPQRST02",
        executionVenue: "XPAR"
    }
}

find {
    complianceResult: CanExecuteUnderMiFIDII
}""").firstTypedObject()
         stub.calls
         result
      }
   }
})
