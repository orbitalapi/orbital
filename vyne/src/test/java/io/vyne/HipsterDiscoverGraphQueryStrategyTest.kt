package io.vyne

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import kotlin.time.ExperimentalTime

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
   fun `Discover required type from relevant service`() = runBlocking {
      val schema = """
         type Isin inherits String
         type NotionalValue inherits Decimal
         model Input {
            isin: Isin
         }

         model Output {
            notionalValue: NotionalValue
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
      result.rawResults
         .test {
            expectRawMap().should.equal( mapOf("notionalValue" to BigDecimal("100")))
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
}
