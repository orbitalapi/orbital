package io.vyne

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.models.TypedInstance
import io.vyne.models.json.parseJson
import io.vyne.models.json.parseJsonModel
import io.vyne.query.connectors.OperationResponseHandler
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

@ExperimentalTime
@ExperimentalCoroutinesApi
class HipsterDiscoverGraphQueryStrategyTest {
   @Test
   fun `Discover required type from a service returning child type of required type`() = runBlocking {
      val schema = """
         type Isin inherits String
         type NotionalValue inherits Decimal
         type InstrumentNotionalValue inherits NotionalValue
         model Input {
            isin: Isin
         }

         model Output {
            notionalValue: NotionalValue
         }

         model Instrument {
             instrumentNotionalValue: InstrumentNotionalValue
         }

         @DataSource
         service HelperService {
            operation `findAll`( ) : Input[]

         }

         service InstrumentService  {
             operation getInstrument( isin : Isin) : Instrument
         }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "`findAll`", vyne.parseJsonModel(
            "Input[]", """
         [
            {  "isin": "isin1"}
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse(
         "getInstrument", vyne.parseJsonModel(
            "Instrument",
            """
              {"instrumentNotionalValue": 100}
         """.trimIndent()
         )
      )

      val result = vyne.query(
         """
            findAll {
                Input[]
              } as Output[]
            """.trimIndent()
      )

      result.rawResults.test {
         expectRawMap().should.equal(mapOf("notionalValue" to BigDecimal("100")))
         expectComplete()
      }
   }

  @Test
   fun `Should not discover required type from relevant service return parent of required type`() = runBlocking {
      val schema = """
         type Isin inherits String
         type NotionalValue inherits Decimal
         type InstrumentNotionalValue inherits NotionalValue
         model Input {
            isin: Isin
         }

         model Output {
            notionalValue: InstrumentNotionalValue
         }

         model Instrument {
             instrumentNotionalValue: NotionalValue
         }

         @DataSource
         service HelperService {
            operation `findAll`( ) : Input[]

         }

         service InstrumentService  {
             operation getInstrument( isin : Isin) : Instrument
         }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "`findAll`", vyne.parseJsonModel(
            "Input[]", """
         [
            {  "isin": "isin1"}
         ]
         """.trimIndent()
         )
      )

      stubService.addResponse(
         "getInstrument", vyne.parseJsonModel(
            "Instrument",
            """
              {"instrumentNotionalValue": 100}
         """.trimIndent()
         )
      )

      val result = vyne.query(
         """
            findAll {
                Input[]
              } as Output[]
            """.trimIndent()
      )

      result.rawResults.test {
         val expected = mapOf("notionalValue" to null)
         expectRawMap().should.equal(expected)
         expectComplete()
      }
   }

   @Test
      /**
       * Our expectation is set the notionalValue field of Output to 100
       * so, we're looking for a value of NotionalValue
       *
       * There are multiple paths in tests
       *
       * Path 1: Fails as InstrumentService@@getNotional  always throws.
       * Input::isin -> NotionalValueRequest -> InstrumentService@@getNotional
       *
       * Path 2: Fails as ReferenceDataService@@keyToValue is invoked with 'null' value.
       * Input::StrategyId -> StrategyService@@getStrategy -> Strategy::notionalValueKey -> ReferenceDataService@@keyToValue
       *
       * Path 3: Fails as  ReferenceDataService@@keyToValue throws for NationalValueKey = 2
       * Input/tradeId ->  TradeDataService@@getTradeData -> TradeData/notionalValueKey -> ReferenceDataService@@keyToValue
       *
       * Path 4: Is the successful Path
       * Input::isin -> ReferenceDataService@getReferenceData -> ReferenceData/notionalValueKey -> ReferenceDataService@@keyToValue
       */
   fun `Discover correct path when there are service failures in discovered paths`() = runBlocking {
      val schema = """
      type Isin inherits String
      type NotionalValueKey inherits Int
      type NotionalValue inherits Decimal
      type StrategyId inherits String
      type TradeId inherits String
      model Input {
         isin: Isin
         strategyId: StrategyId
         tradeId: TradeId
      }

      parameter model NotionalValueRequest {
        isin: Isin
      }

      model NotionalValueResponse {
        notionalValue: NotionalValue
      }

      model Output {
         notionalValue: NotionalValue
      }

      model Instrument {
          instrumentNotionalValue: NotionalValue
      }

      model Strategy {
         notionalValueKey: NotionalValueKey?
      }

      model ReferenceData {
         notionalValueKey: NotionalValueKey
      }

      model NotionalKeyAndValue {
         key: NotionalValueKey
         value: NotionalValue
      }

      model TradeData {
          tradeId: TradeId
          notionalValueKey: NotionalValueKey
      }

      @DataSource
      service HelperService {
         operation `findAll`( ) : Input[]

      }

      service TradeDataService {
         operation getTradeData(tradeId: TradeId): TradeData
      }

      service StrategyService {
         operation getStrategy(id: StrategyId): Strategy
      }

      service InstrumentService  {
          operation getNotional( request : NotionalValueRequest) : NotionalValueResponse
      }

      service ReferenceDataService {
          operation getReferenceData( isin: Isin): ReferenceData
          operation keyToValue(key: NotionalValueKey): NotionalKeyAndValue
      }
      """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "`findAll`", vyne.parseJson(
         "Input[]", """
      [
         {  "isin": "isin1", "strategyId": 1, "tradeId": "trade123"}
      ]
      """.trimIndent()
      )
      )

      // this is invoked for
      // Input::ISIN -> NotionalValueRequest -> getNotional -> NotionalValueResponse::NotionalValue
      // we fail on getNotional operation.
      val getNotionalHandler: OperationResponseHandler = { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
         throw IllegalArgumentException("")
      }

      stubService.addResponse("getNotional",getNotionalHandler)

      // this is invoked for
      // Input::TradeId -> getTradeData -> TradeData::NotionalValueKey -> keyToValue operation
      // note that we return notionalValueKey value as '2' for which keyToValue function will throw exception (see below)
      stubService.addResponse("getTradeData",
         vyne.parseJson(
            "TradeData",
            """
           {"tradeId": "trade123", "notionalValueKey": 2}
      """.trimIndent()
         ))

      // Below invoked through Input::StrategyId -> getStrategy -> Strategy::NotionalValueKey -> keyToValue path.
      // operation returns successfully, but the strategy id is null, so the path can't succeed.
      stubService.addResponse("getStrategy",
         vyne.parseJson(
            "Strategy",
            """
           {"id": null}
      """.trimIndent()
         ))

      // This supposed to be invoked through
      // Input::Isin -> getReferenceData operation -> ReferenceData::NotionalValueKey -> keyToValue operation -> NotionalKeyAndValue::NotionalValue
      val getReferenceDataHandler: OperationResponseHandler = { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
         listOf( vyne.parseJson(
            "ReferenceData",
            """
           {"notionalValueKey": 1}
      """.trimIndent()
         ))
      }
      stubService.addResponse("getReferenceData",getReferenceDataHandler)

      val keyToValueHandler: OperationResponseHandler = { _: RemoteOperation, params: List<Pair<Parameter, TypedInstance>> ->
         // when the parameter value is 2, we arrive here through the tradeId attribute of the 'Input'
         // from tradeId we call getTradeData to fetch the TradeData which has a NotionalValueKey property.
         // we fail here to push the discovery to find
         // Input::Isin -> getReferenceData -> ReferenceData::NotionalValueKey -> keyToValue
         if (params.first().second.value == 2) {
            throw IllegalArgumentException("2")
         }

         if (params.first().second.value == null) {
            throw IllegalArgumentException("null key value")
         }
         listOf( vyne.parseJsonModel(
            "NotionalKeyAndValue",
            """
           {"key": 1, "value": 100}
      """.trimIndent()
         ))
      }

      stubService.addResponse("keyToValue", keyToValueHandler)

      val result = vyne.query(
         """
         findAll {
             Input[]
           } as Output[]
         """.trimIndent()
      )
      result.rawResults
         .test(timeout = Duration.ofHours(1).toKotlinDuration()) {
            expectRawMap().should.equal( mapOf("notionalValue" to BigDecimal("100")))
            expectComplete()
         }

   }

   @Test
      /**
       * Our expectation is set the notionalValue field of Output to 100
       * so, we're looking for a value of NotionalValue
       *
       * There are multiple paths in tests
       *
       * Path 1: Fails as InstrumentService@@getNotional  always throws.
       * Input::isin -> NotionalValueRequest -> InstrumentService@@getNotional
       *
       * Path 2: Fails as ReferenceDataService@@keyToValue is invoked with 'null' value.
       * Input::StrategyId -> StrategyService@@getStrategy -> Strategy::notionalValueKey -> ReferenceDataService@@keyToValue
       *
       * Path 3: Fails as  TradeData notionalValue returns null.
       * Input/tradeId ->  TradeDataService@@getTradeData -> TradeData/notionalValue
       *
       * Path 4: Is the successful Path
       * Input::isin -> ReferenceDataService@getReferenceData -> ReferenceData/notionalValueKey -> ReferenceDataService@@keyToValue
       */
   fun `Discover correct path when there are service failures in discovered paths for a FirstNotEmpty attribute`() = runBlocking {
      val schema = """
      type Isin inherits String
      type NotionalValueKey inherits Int
      type NotionalValue inherits Decimal
      type StrategyId inherits String
      type TradeId inherits String
      model Input {
         isin: Isin
         strategyId: StrategyId
         tradeId: TradeId
      }

      parameter model NotionalValueRequest {
        isin: Isin
      }

      model NotionalValueResponse {
        notionalValue: NotionalValue
      }

      model Output {
         @FirstNotEmpty
         notionalValue: NotionalValue
      }

      model Instrument {
          instrumentNotionalValue: NotionalValue
      }

      model Strategy {
         notionalValueKey: NotionalValueKey?
      }

      model ReferenceData {
         notionalValueKey: NotionalValueKey
      }

      model NotionalKeyAndValue {
         key: NotionalValueKey
         value: NotionalValue
      }

      model TradeData {
          tradeId: TradeId
          notionalValue: NotionalValue?
      }

      @DataSource
      service HelperService {
         operation `findAll`( ) : Input[]

      }

      service TradeDataService {
         operation getTradeData(tradeId: TradeId): TradeData
      }

      service StrategyService {
         operation getStrategy(id: StrategyId): Strategy
      }

      service InstrumentService  {
          operation getNotional( request : NotionalValueRequest) : NotionalValueResponse
      }

      service ReferenceDataService {
          operation getReferenceData( isin: Isin): ReferenceData
          operation keyToValue(key: NotionalValueKey): NotionalKeyAndValue
      }
      """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "`findAll`", vyne.parseJson(
         "Input[]", """
      [
         {  "isin": "isin1", "strategyId": 1, "tradeId": "trade123"}
      ]
      """.trimIndent()
      )
      )

      // this is invoked for
      // Input::ISIN -> NotionalValueRequest -> getNotional -> NotionalValueResponse::NotionalValue
      // we fail on getNotional operation.
      val getNotionalHandler: OperationResponseHandler = { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
         throw IllegalArgumentException("")
      }


      stubService.addResponse("getNotional",getNotionalHandler)

      // this is invoked for
      // Input::TradeId -> getTradeData -> TradeData::NotionalValue
      // note that we return notionalValue value as null which violates @FirstNotEmpty constraint on output property.
      stubService.addResponse("getTradeData",
         vyne.parseJson(
            "TradeData",
            """
           {"tradeId": "trade123", "notionalValue": null}
      """.trimIndent()
         ))

      // Below invoked through Input::StrategyId -> getStrategy -> Strategy::NotionalValueKey -> keyToValue path.
      // operation returns successfully, but the strategy id is null, so the path can't succeed.
      stubService.addResponse("getStrategy",
         vyne.parseJson(
            "Strategy",
            """
           {"id": null}
      """.trimIndent()
         ))

      // This supposed to be invoked through
      // Input::Isin -> getReferenceData operation -> ReferenceData::NotionalValueKey -> keyToValue operation -> NotionalKeyAndValue::NotionalValue
      val getReferenceDataHandler: OperationResponseHandler = { _: RemoteOperation, _: List<Pair<Parameter, TypedInstance>> ->
         listOf( vyne.parseJson(
            "ReferenceData",
            """
           {"notionalValueKey": 1}
      """.trimIndent()
         ))
      }
      stubService.addResponse("getReferenceData",getReferenceDataHandler)

      val keyToValueHandler: OperationResponseHandler = { _: RemoteOperation, params: List<Pair<Parameter, TypedInstance>> ->
         // when the parameter value is 2, we arrive here through the tradeId attribute of the 'Input'
         // from tradeId we call getTradeData to fetch the TradeData which has a NotionalValueKey property.
         // we fail here to push the discovery to find
         // Input::Isin -> getReferenceData -> ReferenceData::NotionalValueKey -> keyToValue
         if (params.first().second.value == 2) {
            throw IllegalArgumentException("2")
         }

         if (params.first().second.value == null) {
            throw IllegalArgumentException("null key value")
         }
         listOf( vyne.parseJsonModel(
            "NotionalKeyAndValue",
            """
           {"key": 1, "value": 100}
      """.trimIndent()
         ))
      }

      stubService.addResponse("keyToValue", keyToValueHandler)

      val result = vyne.query(
         """
         findAll {
             Input[]
           } as Output[]
         """.trimIndent()
      )
      result.rawResults
         .test(timeout = Duration.ofHours(1).toKotlinDuration()) {
            expectRawMap().should.equal( mapOf("notionalValue" to BigDecimal("100")))
            expectComplete()
         }

   }
}
