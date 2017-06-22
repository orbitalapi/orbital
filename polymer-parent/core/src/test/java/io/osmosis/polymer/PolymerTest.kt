package io.osmosis.polymer

import com.winterbe.expekt.expect
import io.osmosis.polymer.models.json.JsonModel
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import org.junit.Test

class PolymerTest {

   val taxiDef = """
namespace polymer.example
type Invoice {
   clientId : ClientId
}
type Client {
   clientId : String as ClientId
   name : String as ClientName
   isicCode : String as IsicCode
}
"""

   @Test
   fun shouldFindAPropertyOnAnObject() {

      val polymer = Polymer()
      polymer.addSchema(TaxiSchema.from(taxiDef))

      val client = """
{
   "clientId" : "123",
   "name" : "Jimmy's Choos",
   "isicCode" : "retailer"
}"""
      polymer.addData(JsonModel(client, typeName = "polymer.example.Client"))
      val result = polymer.resolve("polymer.example.ClientId")
      expect(result.result).to.equal("123")
   }


   @Test
   fun shouldFindAPropertyValueByWalkingADirectRelationship() {

      val polymer = Polymer()
      polymer.addSchema(TaxiSchema.from(taxiDef))

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
      polymer.addData(JsonModel(client, typeName = "polymer.example.Client"))
      val result = polymer.from(JsonModel(invoice, typeName = "polymer.example.Invoice")).find("polymer.example.ClientName")
      expect(result.result).to.equal("Jimmy's Choos")
   }


}



