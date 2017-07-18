package io.osmosis.polymer.query

import com.winterbe.expekt.expect
import io.osmosis.polymer.TestSchema
import io.osmosis.polymer.models.json.addJsonModel
import org.junit.Test

class ModelsScanStrategyTest {
   val polymer = TestSchema.polymer()
   @Test
   fun given_targetIsPresentInContext_then_itIsFound() {
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      polymer.addJsonModel("polymer.example.Client", json)
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("polymer.example.ClientId"), TestSchema.queryContext())
      expect(result.matchedNodes).size.to.equal(1)
      expect(result.matchedNodes.entries.first().key.type.name.fullyQualifiedName).to.equal("polymer.example.ClientId")
      expect(result.matchedNodes.entries.first().value!!.value).to.equal("123")
   }

   @Test
   fun given_targetIsNotPresentInContext_then_emptyListIsReturned() {
      val json = """{ "name" : "Jimmy's Choos" }"""
      polymer.addJsonModel("polymer.example.Client", json)
      val result = ModelsScanStrategy().invoke(TestSchema.typeNode("polymer.example.ClientId"), TestSchema.queryContext())
      expect(result.matchedNodes).to.be.empty
   }
}
