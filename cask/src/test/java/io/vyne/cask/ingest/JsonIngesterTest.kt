package io.vyne.cask.ingest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.cask.config.CaskPostgresJacksonModule
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.query.BaseCaskIntegrationTest
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.math.BigDecimal

class JsonIngesterTest : BaseCaskIntegrationTest() {

   @Test
   fun canIngestJsonArrayWithJsonPath() {
      schemaProvider.updateSource("""
      model Person {
         firstName : String by jsonPath("$.name")
         ageInYears : Int by jsonPath("$.age")
         bal : Decimal by jsonPath("$.balance")
         isActive : Boolean by jsonPath("$.active")
      }
   """)
      ingestJsonData("""[
{"name" : "Marty", "age" : 35, "balance" : 500.21, "active" : true },
{"name" : "Jimmy", "age" : 35, "balance" : 500.21, "active" : true }]""".trimMargin(), versionedType("Person"), taxiSchema)
      val allRecords = caskDao.findAll(versionedType("Person"))
      allRecords.should.have.size(2)
      allRecords[0].let { map ->
         map["firstName"].should.equal("Marty")
         map["ageInYears"].should.equal(35)
         map["isActive"].should.equal(true)
         (map["bal"] as BigDecimal).stripTrailingZeros().should.equal(500.21.toBigDecimal())
      }
      allRecords[1]["firstName"].should.equal("Jimmy")
   }

   @Test
   fun canIngestJsonNodeWithJsonPath() {
      schemaProvider.updateSource("""
      model Person {
         firstName : String by jsonPath("$.name")
         ageInYears : Int by jsonPath("$.age")
         bal : Decimal by jsonPath("$.balance")
         isActive : Boolean by jsonPath("$.active")
      }
   """)
      ingestJsonData("""{"name" : "Marty", "age" : 35, "balance" : 500.21, "active" : true }""".trimMargin(), versionedType("Person"), taxiSchema)
      val allRecords = caskDao.findAll(versionedType("Person"))
      allRecords.should.have.size(1)
      allRecords[0].let { map ->
         map["firstName"].should.equal("Marty")
         map["ageInYears"].should.equal(35)
         map["isActive"].should.equal(true)
         (map["bal"] as BigDecimal).stripTrailingZeros().should.equal(500.21.toBigDecimal())
      }
   }

   @Test
   fun canIngestJsonArrayWithImplicitMappings() {
      schemaProvider.updateSource("""
      model Person {
         name : String
         age : Int
         balance : Decimal
         active : Boolean
      }
   """)
      ingestJsonData("""[
{"name" : "Marty", "age" : 35, "balance" : 500.21, "active" : true },
{"name" : "Jimmy", "age" : 35, "balance" : 500.21, "active" : true }]""".trimMargin(), versionedType("Person"), taxiSchema)
      val allRecords = caskDao.findAll(versionedType("Person"))
      allRecords.should.have.size(2)
      allRecords[0].let { map ->
         map["name"].should.equal("Marty")
         map["age"].should.equal(35)
         map["active"].should.equal(true)
         (map["balance"] as BigDecimal).stripTrailingZeros().should.equal(500.21.toBigDecimal())
      }
      allRecords[1]["name"].should.equal("Jimmy")
   }

   @Test
   fun canIngestJsonNodeWithImplicitMappings() {
      schemaProvider.updateSource("""
      model Person {
         name : String
         age : Int
         balance : Decimal
         active : Boolean
      }
   """)
      ingestJsonData("""{"name" : "Marty", "age" : 35, "balance" : 500.21, "active" : true }""".trimMargin(), versionedType("Person"), taxiSchema)
      val allRecords = caskDao.findAll(versionedType("Person"))
      allRecords.should.have.size(1)
      allRecords[0].let { map ->
         map["name"].should.equal("Marty")
         map["age"].should.equal(35)
         map["active"].should.equal(true)
         (map["balance"] as BigDecimal).stripTrailingZeros().should.equal(500.21.toBigDecimal())
      }
   }

   @Test
   fun `can ingest and query json with complex objects`() {
      schemaProvider.updateSource("""
         type StreetName inherits String
         type CityName inherits String
         type Postcode inherits String
         model GeographicRegion {
            city : CityName
            postCode : Postcode
         }

         model Address {
            houseNumber : Int
            streetName : StreetName
            region : GeographicRegion
         }
         model Person {
            name : String
            address : Address
         }
      """.trimIndent())

      ingestJsonData("""
         {
            "name" : "Jimmy",
            "address" : {
               "houseNumber" : 23,
               "streetName" : "Oxford Street",
               "region" : {
                  "city" : "London",
                  "postCode" : "NW18 5PP"
               }
            }
         }
      """.trimIndent(), versionedType("Person"), taxiSchema)
      val allRecords = caskDao.findAll(versionedType("Person"))
      allRecords.should.have.size(1)
      val firstPerson = allRecords.first()
      val caskRecordId = firstPerson[PostgresDdlGenerator.MESSAGE_ID_COLUMN_NAME]!!
      val jsonFromDb = jacksonObjectMapper()
         .registerModule(CaskPostgresJacksonModule())
         .writeValueAsString(allRecords.first())
      val expected = """{
  "name": "Jimmy",
  "address": {
    "region": {
      "city": "London",
      "postCode": "NW18 5PP"
    },
    "streetName": "Oxford Street",
    "houseNumber": 23
  },
  "caskmessageid": "$caskRecordId"
}"""
      JSONAssert.assertEquals(expected,jsonFromDb, true)
   }

}
