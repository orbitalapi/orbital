package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.json.addJsonModel
import io.vyne.query.QueryResult
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class VyneEnumTest {

   val enumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys("bankbuys"),
                     BankSells("banksells")
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY("buy") synonym of common.BankDirection.BankBuys,
                        SELL("sell") synonym of common.BankDirection.BankSells
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

   @Test
   fun `should build by using synonyms`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : "BUY" } """)

      // When
      val queryResult = vyne.query().build("common.CommonOrder")

      // Then
      queryResult.shouldHaveResults(mapOf("direction" to "BankBuys"))

   }

   @Test
   fun `should build by using synonyms value`() {

      val (vyne, stubService) = testVyne(enumSchema)

      // Query by enum value
      vyne.addJsonModel("BankDirection", """ { "name": "bankbuys" } """)
      val queryResult = vyne.query().build("BankOrder")

      queryResult.shouldHaveResults(mapOf("buySellIndicator" to "buy"))

   }

   @Test
   fun `should build by using synonyms name`() {

      val (vyne, stubService) = testVyne(enumSchema)

      // Query by enum name
      vyne.addJsonModel("BankDirection", """ { "name": "BankSells" } """)
      val queryResultName = vyne.query().build("BankOrder")

      queryResultName.shouldHaveResults(mapOf("buySellIndicator" to "SELL"))
   }

   @Test
   fun `should build by using synonyms value and name different than String`() {

      val enumSchema = TaxiSchema.from("""
                namespace common {
                   enum BankDirection {
                     BankBuys(1),
                     BankSell(2)
                   }

                   model CommonOrder {
                      direction: BankDirection
                   }
                }
                namespace BankX {
                   enum BankXDirection {
                        BUY(3) synonym of common.BankDirection.BankBuys,
                        SELL(4) synonym of common.BankDirection.BankSell
                   }
                   model BankOrder {
                      buySellIndicator: BankXDirection
                   }
                }

      """.trimIndent())

      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : 3 } """)

      // When
      val queryResult = vyne.query().build("common.CommonOrder")

      // Then
      queryResult.shouldHaveResults(
         mapOf("direction" to 1)
      )
   }

   @Test
   fun `should build by using synonyms with vyneql`() {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel("BankX.BankOrder[]", """ [ { "buySellIndicator" : "BUY" }, { "buySellIndicator" : "SELL" } ] """.trimIndent())
      vyne.addJsonModel("BankX.BankOrder[]", """ [ { "buySellIndicator" : "buy" }, { "buySellIndicator" : "sell" } ] """.trimIndent())

      // When
      val queryResult = vyne.query(""" findOne { BankOrder[] } as CommonOrder[] """)

      // Then
      queryResult.shouldHaveResults(
            mapOf("direction" to "bankbuys"),
            mapOf("direction" to "banksells"),
            mapOf("direction" to "BankBuys"),
            mapOf("direction" to "BankSells")
      )
   }

   private fun QueryResult.shouldHaveResults(vararg expectedRawResults: Any) {
      val results = this.results.values.map { it!!.toRawObject() }.toList()
      val flattenedResult = results.flatMap { if (it is List<*>) it.toList() else listOf(it) }
      flattenedResult.size.should.equal(expectedRawResults.size)
      expectedRawResults.forEach { flattenedResult.should.contain(it) }
   }
}

