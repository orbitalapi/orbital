package io.vyne.query

//import io.vyne.TestSchema
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.TestSchema
import io.vyne.Vyne
import io.vyne.models.json.addJsonModel
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedInstances
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Ignore
import org.junit.Test

@ExperimentalCoroutinesApi
class ModelsScanStrategyTest {
   val vyne = TestSchema.vyne()

   @Test
   fun `when context contains an array of a type it is discoverable`():Unit = runBlocking {
      val (vyne, _) = testVyne("""
       model Entry {
          weight:Weight inherits Int
          score:Score inherits Int
       }


       """)
      val inputs = vyne.parseJson("Entry[]", """[ { "weight" : 10 , "score" : 5},  {"weight" : 2 , "score" : 100}]""")
      val result = vyne.from(inputs).build("Entry[]")
         .typedInstances()
      result.should.not.be.empty

   }
   @Test
   fun given_targetIsPresentInContext_then_itIsFound() = runBlockingTest {
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      vyne.addJsonModel("vyne.example.Client", json)
      val result = ModelsScanStrategy().invoke(
         TestSchema.typeNode("vyne.example.ClientId"),
         vyne.query(),
         InvocationConstraints.withAlwaysGoodPredicate
      )
      expect(result.matchedNodes.toList()).size.to.equal(1)
      expect(result.matchedNodes.first().type.name.fullyQualifiedName).to.equal("vyne.example.ClientId")
      expect(result.matchedNodes.first().value).to.equal("123")
   }

   @Test
   fun given_targetIsNotPresentInContext_then_emptyListIsReturned() = runBlockingTest {
      val json = """{ "name" : "Jimmy's Choos" }"""
      vyne.addJsonModel("vyne.example.Client", json)
      vyne.queryEngine()
      val result = ModelsScanStrategy().invoke(
         target = TestSchema.typeNode("vyne.example.ClientId"),
         context = vyne.query(),
         invocationConstraints = InvocationConstraints.withAlwaysGoodPredicate
      )
      // TODO : This test is old. Why are we returning emptyList here, rather than TypedNull?
      expect(result.matchedNodes.toList()).to.be.empty
   }


   @Test
   @Ignore("Need to decide how we indicate failure now - LENS-526")
   fun when_addingComponentType_then_itsFieldsAreNotDiscoverable() = runBlockingTest {
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
      val resultList = result.results.toList()
      resultList.should.not.be.empty
      expect(result.isFullyResolved).to.be.`false`
   }



}
