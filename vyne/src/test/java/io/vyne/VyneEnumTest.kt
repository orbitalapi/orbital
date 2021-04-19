package io.vyne

import app.cash.turbine.test
import com.winterbe.expekt.should
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.query.QueryEngineFactory
import io.vyne.query.QueryResult
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Ignore
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class VyneEnumTest {

   val enumSchema = TaxiSchema.from(
      """
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

      """.trimIndent()
   )

   @Test
   @Ignore("Should enabled when Enum inheritance is supported")
   fun `enum inheritance should be calculated`() {
      val schema = TaxiSchema.from(
         """
          enum EnumA {
            A1,
            A2
         }

         enum EnumA1 inherits EnumA

         enum EnumB {
            B1 synonym of EnumA.A1,
            B2 synonym of EnumA.A2
         }
         enum EnumB1 inherits EnumB
      """.trimIndent()
      )
      val enumA1 = schema.type("EnumA1")
      enumA1.inherits.should.have.size(1)
      enumA1.inheritsFromTypeNames.should.have.elements("EnumA".fqn())
   }

   @Test
   fun `should build by using synonyms`() = runBlockingTest {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : "buy" } """
      )

      // When
      val queryResult = vyne.query().build("common.CommonOrder")

      // Then - BankBuys("bankbuys")  so we pick the value, i.e. "bankbuys"
      queryResult.shouldHaveResults(mapOf("direction" to "bankbuys"))

   }

   @Test
   fun `should build by using synonyms value`() = runBlockingTest {

      val (vyne, stubService) = testVyne(enumSchema)

      // Query by enum value
      vyne.addJsonModel("BankDirection", """ { "name": "bankbuys" } """)
      val queryResult = vyne.query().build("BankOrder")

      queryResult.shouldHaveResults(mapOf("buySellIndicator" to "buy"))

   }

   @Test
   fun `should build by using synonyms name`() = runBlockingTest {

      val (vyne, stubService) = testVyne(enumSchema)

      // Query by enum name
      vyne.addJsonModel("BankDirection", """ { "name": "BankSells" } """)
      val queryResultName = vyne.query().build("BankOrder")

      queryResultName.shouldHaveResults(mapOf("buySellIndicator" to "SELL"))
   }

   @Test
   fun `should build by using synonyms value and name different than String`() = runBlockingTest {

      val enumSchema = TaxiSchema.from(
         """
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

      """.trimIndent()
      )

      val (vyne, _) = testVyne(enumSchema)
      vyne.addJsonModel(
         "BankX.BankOrder", """ { "buySellIndicator" : 3 } """
      )

      // When
      val queryResult = vyne.query().build("common.CommonOrder")

      // Then
      queryResult.rawResults.test {
         expectRawMap().should.equal(
            mapOf("direction" to 1)
         )
         expectComplete()
      }
   }

   @Test
   fun `should build by using synonyms with vyneql`() = runBlocking {

      // Given
      val (vyne, stubService) = testVyne(enumSchema)

      vyne.addJsonModel(
         "BankX.BankOrder[]",
         """ [ { "buySellIndicator" : "BUY" }, { "buySellIndicator" : "SELL" } ] """.trimIndent()
      )
      vyne.addJsonModel(
         "BankX.BankOrder[]",
         """ [ { "buySellIndicator" : "buy" }, { "buySellIndicator" : "sell" } ] """.trimIndent()
      )

      // When
      val queryResult = vyne.query(""" findAll { BankOrder[] } as CommonOrder[] """)

      // I don't undersstand why this doesn't work using turbine.
//      val resultList = queryResult.results.toList()
//         .map { it.toRawObject() }
//      resultList.should.equal(listOf(
//         mapOf("direction" to "bankbuys"),
//         mapOf("direction" to "banksells"),
//         mapOf("direction" to "BankBuys"),
//         mapOf("direction" to "BankSells")
//      ))
      // Then

      queryResult.rawResults.test(Duration.INFINITE) {
         // Don't understand why this isn't working.
         // calling
         expectRawMap().should.equal(mapOf("direction" to "bankbuys"))
         expectRawMap().should.equal(mapOf("direction" to "banksells"))
         expectRawMap().should.equal(mapOf("direction" to "BankBuys"))
         expectRawMap().should.equal(mapOf("direction" to "BankSells"))
         expectComplete()
         // inside this is failing.
//         expectManyRawMaps(4).should.equal(listOf(
//            mapOf("direction" to "bankbuys"),
//            mapOf("direction" to "banksells"),
//            mapOf("direction" to "BankBuys"),
//            mapOf("direction" to "BankSells")
//         ))
//         expectComplete()
      }
   }

   @Test
   @Ignore("Enum inheritence not supported yet")
   fun when_enumWithSynonymIsPresent_then_itCanBeFound() = runBlockingTest {
      val taxiDef = """
         enum EnumA {
            A1,
            A2
         }

         enum EnumA1 inherits EnumA

         enum EnumB {
            B1 synonym of EnumA.A1,
            B2 synonym of EnumA.A2
         }
         enum EnumB1 inherits EnumB
      """.trimIndent()
      val schema = TaxiSchema.from(taxiDef)
      val vyne = Vyne(QueryEngineFactory.default()).addSchema(schema)
      vyne.addKeyValuePair("EnumA1", "A1")
      val result = vyne.query().find("EnumB1")
      vyne.query().findFirstBlocking("EnumB").value.should.equal("B1")
      vyne.query().findFirstBlocking("EnumB1").value.should.equal("B1")
      vyne.query().findFirstBlocking("EnumA").value.should.equal("A1")
      vyne.query().findFirstBlocking("EnumA1").value.should.equal("A1")
   }

   @Test
   @Ignore("Should enabled when Enum inheritance is supported")
   fun `should project by using synonyms and inheritance`() = runBlocking {

      val enumSchema = TaxiSchema.from(
         """
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

      """.trimIndent()
      )

      // Given
      val (vyne, stubService) = testVyne(enumSchema)
      vyne.addJsonModel("typeA", """ { "fieldA1" : "AAA2" } """.trimIndent())

      // When
      val queryResult = vyne.query(""" findOne { typeA } as typeB """)

      // Then
      queryResult.rawResults.test {
         expectRawMap().should.equal(
            mapOf("fieldB1" to "BBB2")
         )
         expectComplete()
      }
   }

   @Test
   @Ignore("Should enabled when Enum inheritance is supported")
   fun `should project by using synonyms and multiple inheritance`() = runBlocking {

      val enumSchema = TaxiSchema.from(
         """
enum EnumA {
   AAA1,
   AAA2
}

enum EnumA1 inherits EnumA
enum EnumA2 inherits EnumA

enum EnumB {
   BBB1 synonym of EnumA.AAA1,
   BBB2 synonym of EnumA.AAA2
}
enum EnumB1 inherits EnumB
enum EnumB2 inherits EnumB

type typeA {
   fieldA1: EnumA1
   fieldA2: EnumA2
}

type typeB {
   fieldB1: EnumB1
   fieldB2: EnumB2
}

      """.trimIndent()
      )

      // Given
      val (vyne, stubService) = testVyne(enumSchema)

      vyne.addJsonModel("typeB", """ { "fieldB1" : "BBB1" } """.trimIndent())


      // When - One model
      val queryResult = vyne.query(""" findOne { typeB } as typeA """)

      // Then
      //  queryResult.results.test(12.toDuration(DurationUnit.MINUTES)) {
      //    expectComplete()
      //}
      queryResult.shouldHaveResults(mapOf("fieldA1" to "AAA1", "fieldA2" to "AAA1"))

   }

   private suspend fun QueryResult.shouldHaveResults(vararg expectedRawResults: Any) {
      val results = this.results!!.mapNotNull {
         it.toRawObject()
      }
         .toList()

      results.size.should.equal(expectedRawResults.size)

      expectedRawResults.forEach { results.should.contain(it) }

   }


}
