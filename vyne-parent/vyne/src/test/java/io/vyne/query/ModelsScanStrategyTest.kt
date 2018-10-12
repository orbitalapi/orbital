package io.vyne.query

import com.winterbe.expekt.expect
import io.vyne.TestSchema
import io.vyne.models.json.addJsonModel
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
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), TestSchema.queryContext())
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal("vyne.example.ClientId")
      expect(result.matchedNodes.entries.first().value!!.value).to.equal("123")
   }

   @Test
   fun given_targetIsNotPresentInContext_then_emptyListIsReturned() {
      val json = """{ "name" : "Jimmy's Choos" }"""
      vyne.addJsonModel("vyne.example.Client", json)
      vyne.query()
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("vyne.example.ClientId"), TestSchema.queryContext())
      expect(result.matchedNodes).to.be.empty
   }
}
