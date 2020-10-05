package io.vyne.models.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.Vyne
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
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
enum BuildingType {
  House,
  Flat
}
type Client {
   clientId : ClientId as String
   name : ClientName as String
   buildingType : BuildingType
   emails : Email[]
   address : Address
}

type ModelWithNullableFields {
    stringNullable: String? by jsonPath("$.string")
    intNullable: Int? by jsonPath("$.int")
    dateNullable: Date? by jsonPath("$.date")
    id: String by jsonPath("$.id")
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
   "buildingType" : "House",
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

      expect(client["buildingType"].type.name).to.equal(schema.type("BuildingType").name)
      expect(client["buildingType"].type.isEnum).to.be.`true`
      expect(client["buildingType"]!!.value).to.equal("House")

      expect(client["emails"]!!).to.be.instanceof(TypedCollection::class.java)
      val emails = client["emails"] as TypedCollection
      expect(emails).to.have.size(2)
      expect(emails[0].value).to.equal("martypitt@me.com")
      expect(emails[0].type.name).to.equal(schema.type("Email").name)

      expect(client["address"]!!).to.be.instanceof(TypedObject::class.java)
      val address = client["address"] as TypedObject
      expect(address["houseNumber"]!!.value).to.equal(123)
   }

   @Test
   fun parsesJsonNullValuesToTypedObject() {
      val vyne = Vyne(QueryEngineFactory.noQueryEngine()).addSchema(TaxiSchema.from(taxiDef))
      val json = """
{
   "clientId" : null,
   "name" : "Marty Pitt",
   "emails" : ["martypitt@me.com","marty@marty.com"],
   "buildingType" : null,
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
      expect(client["clientId"]!!.value).to.be.`null`

      expect(client["buildingType"].type.name).to.equal(schema.type("BuildingType").name)
      expect(client["buildingType"].type.isEnum).to.be.`true`
      expect(client["buildingType"]!!.value).to.be.`null`

      expect(client["emails"]!!).to.be.instanceof(TypedCollection::class.java)
      val emails = client["emails"] as TypedCollection
      expect(emails).to.have.size(2)
      expect(emails[0].value).to.equal("martypitt@me.com")
      expect(emails[0].type.name).to.equal(schema.type("Email").name)

      expect(client["address"]!!).to.be.instanceof(TypedObject::class.java)
      val address = client["address"] as TypedObject
      expect(address["houseNumber"]!!.value).to.equal(123)
   }

   @Test
   fun `parse json arrays successfully`() {
      val vyne = Vyne(QueryEngineFactory.noQueryEngine()).addSchema(TaxiSchema.from(taxiDef))
      val json = """
[{
   "clientId" : "marty",
   "name" : "Marty Pitt",
   "emails" : ["martypitt@me.com","marty@marty.com"],
   "buildingType" : "House",
   "address" : { "houseNumber" : 123, "street" : "MyStreet" , "postCode" : "SW11 1DN" }
},
{
   "clientId" : "mert",
   "name" : "Marty Pitt",
   "emails" : ["martypitt@me.com","marty@marty.com"],
   "buildingType" : "House",
   "address" : { "houseNumber" : 123, "street" : "MyStreet" , "postCode" : "SW11 1DN" }
}
]

"""

      val schema = vyne.schema
      val parser = JsonModelParser(schema)
      val result = parser.parse(schema.type("Client"), json, source = Provided)

      expect(result).instanceof(TypedCollection::class.java)
      val clients = result as TypedCollection
      clients.size.should.equal(2)
      val client1 = clients[0] as TypedObject
      expect(client1["clientId"]!!.value).to.equal("marty")

      val client2 = clients[1] as TypedObject
      expect(client2["clientId"]!!.value).to.equal("mert")

   }

   @Test
   fun `json parsing handles nullable field definitions correctly`() {
      val vyne = Vyne(QueryEngineFactory.noQueryEngine()).addSchema(TaxiSchema.from(taxiDef))
      val jsonContent = """
         {  "id": "1" }
      """.trimIndent()
      val map = jacksonObjectMapper().readValue<Map<String, Any>>(jsonContent)
      val schema = vyne.schema
      val parsedResult = TypedInstance.from(schema.type("ModelWithNullableFields"), jsonContent, vyne.schema, source = Provided)
      expect(parsedResult).instanceof(TypedObject::class.java)
      val modelWithNullableFields = parsedResult as TypedObject
      expect(modelWithNullableFields.type.name).to.equal(schema.type("ModelWithNullableFields").name)
      expect(modelWithNullableFields["id"].value).to.be.not.`null`
      expect(modelWithNullableFields["intNullable"].value).to.be.`null`
      expect(modelWithNullableFields["dateNullable"].value).to.be.`null`
      expect(modelWithNullableFields["stringNullable"].value).to.be.`null`
   }
}
