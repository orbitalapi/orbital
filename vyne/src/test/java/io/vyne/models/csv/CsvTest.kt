package io.vyne.models.csv

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test


class CsvTest  {

   @Test
   fun canReadCsvData() {
      val src = """type alias FirstName as String
type alias LastName as String
type Person {
   firstName : FirstName by column(1)
   lastName : LastName by column(2)
}
"""
      val (vyne, _) = testVyne(src)
      val csv = "firstName,lastName\n" +
         "jimmy,parsons"
      val parsedResult = TypedInstance.from(vyne.schema.type("Person"), csv, vyne.schema, source = Provided) as TypedObject
      expect(parsedResult.type.fullyQualifiedName).to.equal("Person")
      parsedResult["firstName"].value.should.equal("jimmy")
   }

   @Test
   fun canReadCsvDataWithMultipleRecords() {
      val src = """type alias FirstName as String
type alias LastName as String
type Person {
   firstName : FirstName by column(1)
   lastName : LastName by column(2)
}

@CsvList
type alias PersonList as Person[]
"""
      val (vyne, _) = testVyne(src)
      val csv = "firstName,lastName\n" +
         "jimmy,parsons\n" +
         "olly,spurrs"
      val parsedResult = TypedInstance.from(vyne.schema.type("PersonList"), csv, vyne.schema, source = Provided)
      parsedResult.should.be.instanceof(TypedCollection::class.java)
      val collection = parsedResult as TypedCollection
      collection.should.have.size(2)
   }

   @Test
   fun canBuildListFromCsvDataWIthMultipleRecords() {
      val src = """type alias FirstName as String
type alias LastName as String
type Person {
   firstName : FirstName by column(1)
   lastName : LastName by column(2)
}

@CsvList
type alias PersonList as Person[]

type alias FirstNames as FirstName[]
"""
      val (vyne, _) = testVyne(src)
      val csv = "firstName,lastName\n" +
         "jimmy,parsons\n" +
         "olly,spurrs"
      val parsedResult = TypedInstance.from(vyne.schema.type("PersonList"), csv, vyne.schema, source = Provided)


      runBlocking {

         val buildResult= vyne.query()
            .addFact(parsedResult)
            .build("FirstNames")
            .rawResults.toList()

         buildResult.should.equal(listOf("jimmy","olly"))
      }
   }

   @Test
   fun `can parse using double-quoted column names`() {
      val schema =TaxiSchema.from("""type FirstName inherits String
type LastName inherits String
type Person {
   firstName : FirstName by column("firstName")
   lastName : LastName by column("lastName")
}
""")
      val csv = "firstName,lastName\n" +
         "jimmy,parsons\n" +
         "olly,spurrs"
      val parsed = CsvImporterUtil.parseCsvToType(
         csv,
         CsvIngestionParameters(),
         schema,
         "Person"
      ).map { it.raw as Map<String,Any> }

      parsed.should.equal(listOf(
         mapOf("firstName" to "jimmy", "lastName" to "parsons"),
         mapOf("firstName" to "olly", "lastName" to "spurrs"),
      ))

   }

   @Test
   fun `can parse using single-quoted column names`() {
      val schema =TaxiSchema.from("""type FirstName inherits String
type LastName inherits String
type Person {
   firstName : FirstName by column('firstName')
   lastName : LastName by column('lastName')
}
""")
      val csv = "firstName,lastName\n" +
         "jimmy,parsons\n" +
         "olly,spurrs"
      val parsed = CsvImporterUtil.parseCsvToType(
         csv,
         CsvIngestionParameters(),
         schema,
         "Person"
      ).map { it.raw as Map<String,Any> }
      parsed.should.equal(listOf(
         mapOf("firstName" to "jimmy", "lastName" to "parsons"),
         mapOf("firstName" to "olly", "lastName" to "spurrs"),
      ))
   }


}
