package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.parseJsonModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class HipsterDiscoverGraphQueryStrategyTest {
   @Test
   fun `Discover required type from a service returning child type of required type`() = runBlockingTest {
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
            operation findAll( ) : Input[]

         }

         service InstrumentService  {
             operation getInstrument( isin : Isin) : Instrument
         }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "findAll", vyne.parseJsonModel(
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

      result.rawObjects().should.equal(
         listOf(
            mapOf("notionalValue" to BigDecimal("100"))
         )
      )
   }

   @Test
   fun `Discover required type from relevant service`() = runBlockingTest {
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
            operation findAll( ) : Input[]

         }

         service InstrumentService  {
             operation getInstrument( isin : Isin) : Instrument
         }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "findAll", vyne.parseJsonModel(
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

      result.rawObjects().should.be.equal(
         listOf(
            mapOf("notionalValue" to BigDecimal("100"))
         )
      )
   }

   @Test
   fun `Should not discover required type from relevant service return parent of required type`() = runBlockingTest {
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
            operation findAll( ) : Input[]

         }

         service InstrumentService  {
             operation getInstrument( isin : Isin) : Instrument
         }
         """.trimIndent()
      val (vyne, stubService) = testVyne(schema)
      stubService.addResponse(
         "findAll", vyne.parseJsonModel(
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

      result.rawObjects().should.be.equal(
         listOf(
            mapOf("notionalValue" to null)
         )
      )
   }
}
