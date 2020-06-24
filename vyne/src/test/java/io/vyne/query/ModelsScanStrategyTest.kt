package io.vyne.query

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.TestSchema
import io.vyne.Vyne
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.addKeyValuePair
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class ModelsScanStrategyTest {
   val vyne = TestSchema.vyne()
   @Test
   fun given_targetIsPresentInContext_then_itIsFound() {
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      vyne.addJsonModel("vyne.example.Client", json)
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), vyne.query())
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal("vyne.example.ClientId")
      expect(result.matchedNodes.entries.first().value!!.value).to.equal("123")
   }

   @Test
   fun given_targetIsNotPresentInContext_then_emptyListIsReturned() {
      val json = """{ "name" : "Jimmy's Choos" }"""
      vyne.addJsonModel("vyne.example.Client", json)
      vyne.queryEngine()
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), vyne.query())
      expect(result.matchedNodes).to.be.empty
   }


   @Test
   fun when_addingComponentType_then_itsFieldsAreNotDiscoverable() {
      val taxiDef = """
  closed type Money {
    currency : Currency as String
    value : MoneyAmount as Decimal
 }
      """.trimIndent()
      val schema = TaxiSchema.from(taxiDef)
      val vyne = Vyne(QueryEngineFactory.default()).addSchema(schema)
      vyne.addJsonModel("Money", """{ "currency" : "USD" , "value" : 3000 }""")
      val result = vyne.query().find("Currency")
      expect(result.isFullyResolved).to.be.`false`
   }

   @Test
   fun when_enumWithSynonymIsPresent_then_itCanBeFound() {
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
      vyne.query().find("EnumB")["EnumB"]!!.value.should.equal("B1")
      vyne.query().find("EnumB1")["EnumB1"]!!.value.should.equal("B1")
      vyne.query().find("EnumA")["EnumA"]!!.value.should.equal("A1")
      vyne.query().find("EnumA1")["EnumA1"]!!.value.should.equal("A1")
   }

}
