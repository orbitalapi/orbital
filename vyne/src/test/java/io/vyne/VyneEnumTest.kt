package io.vyne

import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.parseJsonModel
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
         listOf(
            mapOf("direction" to "bankbuys"),
            mapOf("direction" to "banksells"),
            mapOf("direction" to "BankBuys"),
            mapOf("direction" to "BankSells")

         )
      )
   }

   @Test
   fun `should project by using synonyms and inheritance`() {

      val enumSchema = TaxiSchema.from("""
enum EnumA {
   AAA1,
   AAA2
}

enum EnumA1 inherits EnumA

enum EnumB {
   BBB1 synonym of EnumA.AAA1,
   BBB2 synonym of EnumA.AAA2
}
enum EnumB1 inherits EnumB

type typeA {
   fieldA1: EnumA1
}

type typeB {
   fieldB1: EnumB1
}

      """.trimIndent())

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel("typeA", """ { "fieldA1" : "AAA2" } """.trimIndent())

      // When
      val queryResult = vyne.query(""" findOne { typeA } as typeB """)

      // Then
      queryResult.shouldHaveResults(mapOf("fieldB1" to "BBB2"))

   }

   @Test
   fun `should project by using synonyms and multiple inheritance`() {

      val enumSchema = TaxiSchema.from("""
enum EnumA {
   AAA1,
   AAA2
}

type EnumA1 inherits EnumA
type EnumA2 inherits EnumA

enum EnumB {
   BBB1 synonym of EnumA.AAA1,
   BBB2 synonym of EnumA.AAA2
}
type EnumB1 inherits EnumB
type EnumB2 inherits EnumB

type typeA {
   fieldA1: EnumA1
   fieldA2: EnumA2
}

type typeB {
   fieldB1: EnumB1
   fieldB2: EnumB2
}

      """.trimIndent())

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel("typeB", """ { "fieldB1" : "BBB1" } """.trimIndent())


      // When - One model
      val queryResult = vyne.query(""" findOne { typeB } as typeA """)

      // Then
      queryResult.shouldHaveResults( mapOf("fieldA1" to "AAA1", "fieldA2" to "AAA1"))

   }

   private fun QueryResult.shouldHaveResults(vararg expectedRawResults: Any) {
      val results = this.results.values.map { it!!.toRawObject() }.toList()

      results.size.should.equal(expectedRawResults.size)

      expectedRawResults.forEach { results.should.contain(it) }

   }


}

