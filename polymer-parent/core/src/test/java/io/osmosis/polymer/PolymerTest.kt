package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.models.json.addJsonModel
import io.osmosis.polymer.query.QueryContext
import io.osmosis.polymer.query.QueryParser
import io.osmosis.polymer.query.QuerySpecTypeNode
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

object TestSchema {
   val taxiDef = """
namespace polymer.example
type Invoice {
   clientId : ClientId
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   isicCode : IsicCode as String
}
"""
   val schema = TaxiSchema.from(taxiDef)

   val polymer = Polymer().addSchema(schema)
   val queryParser = QueryParser(schema)

   fun typeNode(name: String): Set<QuerySpecTypeNode> {
      return queryParser.parse(name)
   }

   fun queryContext(): QueryContext = polymer.queryContext()
}

class PolymerTest {


   @Test
   fun shouldFindAPropertyOnAnObject() {

      val polymer = TestSchema.polymer
      val json = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      polymer.addJsonModel("polymer.example.Client", json)
      val result = polymer.query().find("polymer.example.ClientName")
      expect(result.values.size).to.equal(1)
      expect(result["polymer.example.ClientName"]!!).to.equal("Jimmy's Choos")
   }


   @Test
   fun shouldFindAPropertyValueByWalkingADirectRelationship() {

      val polymer = TestSchema.polymer
      val client = """
         {
            "clientId" : "123",
            "name" : "Jimmy's Choos",
            "isicCode" : "retailer"
         }"""
      val invoice = """
         {
            "clientId" : "123"
         }
         """
      TODO()
//      polymer.addData(JsonModel(client, typeName = "polymer.example.Client"))
//      val result = polymer.from(JsonModel(invoice, typeName = "polymer.example.Invoice")).find("polymer.example.ClientName")
//      expect(result.result).to.equal("Jimmy's Choos")
   }


}



