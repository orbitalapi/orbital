package io.vyne.models.expressions

import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.models.json.Jackson
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseKeyValuePair
import io.vyne.testVyne
import io.vyne.typedObjects
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

      fun tickerToMetrics(ticker: String): TypedInstance {
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

      val minQuery = """given { isin : Isin = "${isin.value}" } findOne { IsinFundHoldings } as {
         |  scores: FundHoldingWithScore[] by [FundHolding]
         |  portfolioScore : Decimal by min(this.scores, (FundHoldingWithScore) -> EsgAveragedScore)
         |}[]
      """.trimMargin()
      val results = vyne.query(query)
//         .rawObjects()

      val typedInstances = results.typedObjects()
      val score = typedInstances.single()["portfolioScore"].source
      val json = Jackson.defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(score)
      typedInstances.single().get("portfolioScore").value.should.equal(BigDecimal("2.53366"))
   }

   @Test
   fun `can do classification`(): Unit = runBlocking {
      val schema = """
         type EquityName inherits String
         type Isin inherits String
         type Ticker inherits String
         type Cusip inherits String
         type Sedol inherits String
         type Theme inherits String
         type EntityId inherits String
         type OverallGlobalCompactComplianceStatus inherits String
         type ClimateRevenuePct inherits Decimal
         type IssuerId inherits String
         type WeightedAverageScore inherits Decimal
         type Bottom5PctScore inherits Decimal
         type Classification inherits String


         model SlEquity {
            name: EquityName
            isin: Isin
            ticker: Ticker
            cusip: Cusip?
            sedol: Sedol?
            theme: Theme?

         }

         model PurePlayClimateFF {
            isin: Isin
            climateRevenuePct: ClimateRevenuePct

         }

         model ProductInvolvement {
            entityId: EntityId
            isin: Isin
         }

         model GlobalStandardsScreening {
            entityId: EntityId
            complianceStatus: OverallGlobalCompactComplianceStatus

         }

         model PredefinedContentSecurityLevelReference {
            issuerId: IssuerId
            isin: Isin
         }

         model MsciScores {
            issuerId: IssuerId
            weightedAverageScore: WeightedAverageScore
         }

         service SlEquityService {
            operation allSlEquities(): SlEquity[]
         }

         service PurePlayClimateFFService {
           operation climateRevenuePctByIsin(Isin): ClimateRevenuePct
         }
         service ProductInvolvementService {
            operation findByIsin(Isin): ProductInvolvement
         }

         service GlobalStandardsScreeningService {
            operation findByEntityId(EntityId): GlobalStandardsScreening
         }

         service PredefinedContentSecurityLevelReferenceService {
            operation findByIsin2(Isin): PredefinedContentSecurityLevelReference
         }

         service MsciSCoresService {
            operation msciScoreByIssuerId(IssuerId): MsciScores
         }

         service WeightedAverageScorePercentileService {
            operation bottom5PctScore(IssuerId): Bottom5PctScore

         }


      """.trimIndent()
      val (vyne, stub) = testVyne(schema)

      stub.addResponse("allSlEquities") { _, _ ->
         val first = TypedInstance.from(
            vyne.type("SlEquity"), mapOf(
            "isin" to "isin1",
            "ticker" to "ticker1",
            "theme" to "GR Climate List"
         ), vyne.schema)

         val second = TypedInstance.from(
            vyne.type("SlEquity"), mapOf(
            "isin" to "isin2",
            "ticker" to "ticker2",
            "theme" to "GR Climate List"
         ), vyne.schema)


         val third = TypedInstance.from(
            vyne.type("SlEquity"), mapOf(
            "isin" to "isin3",
            "ticker" to "ticker3",
            "theme" to "Energy Transition"
         ), vyne.schema)
         listOf(first, second, third)
      }

      stub.addResponse("findByIsin") { _, params ->
         val isin = params[0].second.value as String
         val isinIndex = isin.last()

         val first = TypedInstance.from(
            vyne.type("ProductInvolvement"), mapOf(
            "isin" to isin,
            "entityId" to "entityId$isinIndex"
         ), vyne.schema)

         listOf(first)
      }

      stub.addResponse("findByEntityId") { _, params ->
         val entityId = params[0].second.value as String
         val resp = when (entityId.last().digitToInt()) {
            1 -> TypedInstance.from(
               vyne.type("GlobalStandardsScreening"), mapOf(
               "complianceStatus" to "Compliant",
               "entityId" to entityId
            ), vyne.schema)

            2 -> TypedInstance.from(
               vyne.type("GlobalStandardsScreening"), mapOf(
               "complianceStatus" to "Compliant",
               "entityId" to entityId
            ), vyne.schema)

            else -> TypedInstance.from(
               vyne.type("GlobalStandardsScreening"), mapOf(
               "complianceStatus" to "foo",
               "entityId" to entityId
            ), vyne.schema)
         }

         listOf(resp)
      }

      stub.addResponse("findByIsin2") { _, params ->
         val isin = params[0].second.value as String
         val isinIndex = isin.last()
         val first = TypedInstance.from(
            vyne.type("PredefinedContentSecurityLevelReference"), mapOf(
            "isin" to isin,
            "issuerId" to "issuerId$isinIndex"
         ), vyne.schema)

         listOf(first)
      }

      stub.addResponse("bottom5PctScore") { _, params ->
         val issuerId = params[0].second.value as String
         val resp = when (issuerId.last().digitToInt()) {
            1 -> TypedInstance.from(
               vyne.type("Bottom5PctScore"), 5.5, vyne.schema)

            2 -> TypedInstance.from(
               vyne.type("Bottom5PctScore"), 3.5, vyne.schema)

            else -> TypedInstance.from(
               vyne.type("Bottom5PctScore"), 4.5, vyne.schema)
         }

         listOf(resp)
      }

      stub.addResponse("msciScoreByIssuerId") { _, params ->
         val issuerId = params[0].second.value as String
         val resp = when (issuerId.last().digitToInt()) {
            1 -> TypedInstance.from(
               vyne.type("MsciScores"), mapOf("weightedAverageScore" to 7, "issuerId" to issuerId), vyne.schema)

            2 -> TypedInstance.from(
               vyne.type("MsciScores"), mapOf("weightedAverageScore" to 8, "issuerId" to issuerId), vyne.schema)

            else -> TypedInstance.from(
               vyne.type("MsciScores"), mapOf("weightedAverageScore" to 1.5, "issuerId" to issuerId), vyne.schema)
         }

         listOf(resp)
      }

      stub.addResponse("climateRevenuePctByIsin") { _, params ->
         val isin = params[0].second.value as String
         val resp = when (isin.last().digitToInt()) {
            1 -> TypedInstance.from(
               vyne.type("ClimateRevenuePct"), 51, vyne.schema)

            2 -> TypedInstance.from(
               vyne.type("ClimateRevenuePct"), 49, vyne.schema)

            else -> TypedInstance.from(
               vyne.type("ClimateRevenuePct"), 33, vyne.schema)
         }

         listOf(resp)
      }

      val results = vyne.query("""
         findAll {
            SlEquity[]
         } as {
            isin: Isin
            theme: Theme
            classification: Classification? by when {
               Theme == 'GR Climate List' &&  OverallGlobalCompactComplianceStatus != 'Non-Compliant' && ClimateRevenuePct > 50 && Bottom5PctScore > 5 -> 'Thematic'
               Theme != 'GR Climate List' -> 'Thematic'
               else -> null
            }
         }[]
      """.trimIndent())

      val typedInstances = results.typedObjects()
      typedInstances.size.should.equal(3)

      typedInstances.map { it.toRawObject() }
         .should.equal(
            listOf<Map<String, Any?>>(
               mapOf("isin" to "isin1", "theme" to "GR Climate List", "classification" to "Thematic"),
               mapOf("isin" to "isin2", "theme" to "GR Climate List", "classification" to null),
               mapOf("isin" to "isin3", "theme" to "Energy Transition", "classification" to "Thematic")
            ))
   }
}
