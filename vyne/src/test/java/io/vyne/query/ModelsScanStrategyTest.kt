package io.vyne.query

import com.winterbe.expekt.expect
//import io.vyne.TestSchema
import io.vyne.Vyne
import io.vyne.models.json.addJsonModel
import io.vyne.schemas.taxi.TaxiSchema
import kotlinx.coroutines.runBlocking
import org.junit.Test

/*
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
      val result = runBlocking {ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal("vyne.example.ClientId")
      expect(result.matchedNodes.entries.first().value!!.value).to.equal("123")
   }

   @Test
   fun given_targetIsNotPresentInContext_then_emptyListIsReturned() {
      val json = """{ "name" : "Jimmy's Choos" }"""
      vyne.addJsonModel("vyne.example.Client", json)
      vyne.queryEngine()
      val result = runBlocking {ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), vyne.query(), InvocationConstraints.withAlwaysGoodPredicate)}
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
      val result = runBlocking {vyne.query().find("Currency")}
      expect(result.isFullyResolved).to.be.`false`
   }

}
*/
