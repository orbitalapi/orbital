package io.vyne.models.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import io.vyne.Vyne
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedObject
import io.vyne.query.QueryEngineFactory
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class JsonModelParserTest {
   val taxiDef = """
type alias Email as String
type Address {
   houseNumber : HouseNumber as Int
   street : StreetName as String
   postCode : PostCode as String
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   emails : Email[]
   address : Address
}
"""
   @Test
   fun parsesJsonObjectToTypedObject() {
      val vyne = Vyne(QueryEngineFactory.noQueryEngine()).addSchema(TaxiSchema.from(taxiDef))
      val json = """
{
   "clientId" : "marty",
   "name" : "Marty Pitt",
   "emails" : ["martypitt@me.com","marty@marty.com"],
   "address" : { "houseNumber" : 123, "street" : "MyStreet" , "postCode" : "SW11 1DN" }
}
"""
      val map = jacksonObjectMapper().readValue<Map<String, Any>>(json)
      val schema = vyne.schema
      val parser = JsonModelParser(schema)
      // Note : Some weirdness with debugging when passing the Json.
      // Looks like a bug in the IntelliJ kotlin plugin, and handling
      // inline operations.  Possibly fixed by the time anyone WTF's this.
      // Ideally, pass the json, not the map.
      val result = parser.doParse(schema.type("Client"), map, source = Provided)

      expect(result).instanceof(TypedObject::class.java)
      val client = result as TypedObject
      expect(client.type.name).to.equal(schema.type("Client").name)
      expect(client["clientId"].type.name).to.equal(schema.type("ClientId").name)
      expect(client["clientId"]!!.value).to.equal("marty")

      expect(client["emails"]!!).to.be.instanceof(TypedCollection::class.java)
      val emails = client["emails"] as TypedCollection
      expect(emails).to.have.size(2)
      expect(emails[0].value).to.equal("martypitt@me.com")
      expect(emails[0].type.name).to.equal(schema.type("Email").name)

      expect(client["address"]!!).to.be.instanceof(TypedObject::class.java)
      val address = client["address"] as TypedObject
      expect(address["houseNumber"]!!.value).to.equal(123)
   }
}
