package io.vyne.models.csv

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.testVyne
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
      val parsedResult = TypedInstance.from(vyne.schema.type("Person"), csv, vyne.schema) as TypedObject
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
      val parsedResult = TypedInstance.from(vyne.schema.type("PersonList"), csv, vyne.schema)
      parsedResult.should.be.instanceof(TypedCollection::class.java)
      val collection = parsedResult as TypedCollection
      collection.should.have.size(2)
   }

   @Test
   fun parsesTradeRecordsCsv() {

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
      val parsedResult = TypedInstance.from(vyne.schema.type("PersonList"), csv, vyne.schema)
      val buildResult = vyne.query()
         .addFact(parsedResult)
         .build("FirstNames")
      val result = buildResult.results.values.first()!!
      result.should.be.instanceof(TypedCollection::class.java)
      val resultCollection = result as TypedCollection
      resultCollection[0].type.fullyQualifiedName.should.equal("FirstName")
      buildResult.resultMap["FirstNames"].should.equal(listOf("jimmy","olly"))
   }

}
