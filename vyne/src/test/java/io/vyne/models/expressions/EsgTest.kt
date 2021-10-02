package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import io.vyne.rawObjects
import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal

class EsgTest {

   @Test
   fun `can do esg example`(): Unit = runBlocking {
      // Isin -> Holdings -> Metrics
      val (vyne, stub) = testVyne(
         """
         type Isin inherits String

         model FundHolding {
            @Id
            ticker : InstrumentTicker inherits String
            percent : HoldingPercentage inherits Decimal
         }

         model InstrumentMetrics {
            @Id
            ticker : InstrumentTicker
            score : EsgAveragedScore inherits Decimal
         }

         model FundHoldingWithScore {
            ticker : InstrumentTicker inherits String
            percent : HoldingPercentage inherits Decimal
            score : EsgAveragedScore inherits Decimal
         }

         model IsinFundHoldings {
            holdings: FundHolding[]
         }

         service Service {
            operation lookupHoldingsWithWrapper(Isin):IsinFundHoldings
            operation lookupHoldings(Isin):FundHolding[]
            // This approach doesn't work, as we can't map FundHolding[] -> { FundHolding -> InstrumentTicker -> InstrumentMetrics } -> InstrumentMetrics[]
            // However, it'd be cool if we could
            operation lookupMetrics(InstrumentTicker):InstrumentMetrics
            operation lookupMultipleMetrics(InstrumentTicker[]):InstrumentMetrics[]
         }
      """.trimIndent()
      )

      val fundHoldingsJson = """[
         |{ "ticker" : "AAPL", "percent"  : 29.47 },
         |{ "ticker" : "MSFT", "percent"  : 3.46 },
         |{ "ticker" : "AMZN", "percent"  : 1.62 },
         |{ "ticker" : "GOOGL", "percent"  : 1.57 },
         |{ "ticker" : "OTHER", "percent"  : 63.88 }
         |]""".trimMargin()
      val holdings = vyne.parseJson(
         "FundHolding[]", fundHoldingsJson
      )
      val holdingsWithWrapper = vyne.parseJson("IsinFundHoldings", """ { "holdings" : $fundHoldingsJson }""")
      stub.addResponse("lookupHoldings", holdings)
      stub.addResponse("lookupHoldingsWithWrapper", holdingsWithWrapper)

      val metrics = mapOf(
         "AMZN" to 4.6,
         "AAPL" to 4.8,
         "MSFT" to 5.1,
         "GOOGL" to 2.4,
         "OTHER" to 1.3
      )
      fun tickerToMetrics(ticker:String): TypedInstance {
         val score = metrics[ticker]!!
         return TypedInstance.from(
            vyne.type("InstrumentMetrics"), mapOf(
               "ticker" to ticker,
               "score" to score
            ), vyne.schema
         )
      }
      stub.addResponse("lookupMetrics") { remoteOperation, params ->
         val ticker = params[0].second.value as String
         listOf(tickerToMetrics(ticker))
      }
      stub.addResponse("lookupMultipleMetrics") { remoteOperation, params ->
         val tickers = params[0].second.value as List<TypedValue>
         val result = tickers.map { ticker -> tickerToMetrics(ticker.value as String) }
         result
      }
      val isinValue = "LU1689525381"
      val isin = vyne.parseKeyValuePair("Isin", isinValue)

      // Some queries
//      val results = vyne.from(isin).find("InstrumentMetrics[]")
//         .typedInstances()
//      results.should.have.size(1)

//      |  portfolioScore : Decimal by sum(this.scores, (FundHolding) -> (HoldingPercentage * EsgAveragedScore)) / sum(this.scores, (FundHolding) -> HoldingPercentage)
      val query = """given { isin : Isin = "${isin.value}" } findOne { IsinFundHoldings } as {
         |  scores: FundHoldingWithScore[] by [FundHolding]
         |  portfolioScore : Decimal by sum(this.scores, (FundHoldingWithScore) -> HoldingPercentage * EsgAveragedScore) / sum(this.scores, (FundHoldingWithScore) -> HoldingPercentage)
         |}[]
      """.trimMargin()
      val results = vyne.query(query)
         .rawObjects()
      results.should.have.size(1)
      results[0]["portfolioScore"]!!.should.equal(BigDecimal("2.53366"))
   }
}
