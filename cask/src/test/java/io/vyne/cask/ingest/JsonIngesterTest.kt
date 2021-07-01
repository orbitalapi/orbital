package io.vyne.cask.ingest

import com.winterbe.expekt.should
import io.vyne.cask.query.BaseCaskIntegrationTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.stream.Collectors

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

       val allRecordsStream = caskDao.findAll(versionedType("Person"))
       val allRecords = allRecordsStream.collect(Collectors.toList())
       allRecordsStream.close()
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

       val allRecordsStream = caskDao.findAll(versionedType("Person"))
      val allRecords = allRecordsStream.collect(Collectors.toList())
       allRecordsStream.close()

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
       val allRecordsStream = caskDao.findAll(versionedType("Person"))
      val allRecords = allRecordsStream.collect(Collectors.toList())
       allRecordsStream.close()
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
       val allRecordsStream = caskDao.findAll(versionedType("Person"))
       val allRecords = allRecordsStream.collect(Collectors.toList())
       allRecordsStream.close()
      allRecords.should.have.size(1)
      allRecords[0].let { map ->
         map["name"].should.equal("Marty")
         map["age"].should.equal(35)
         map["active"].should.equal(true)
         (map["balance"] as BigDecimal).stripTrailingZeros().should.equal(500.21.toBigDecimal())
      }
   }

}
