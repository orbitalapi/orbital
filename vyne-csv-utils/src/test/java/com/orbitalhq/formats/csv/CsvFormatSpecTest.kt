package com.orbitalhq.formats.csv

import com.orbitalhq.VersionedSource
import com.orbitalhq.from
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test

class CsvFormatSpecTest {

   @Test
   fun `can parse with pipe delimiter null value`() {

      val schema = TaxiSchema.from(
         listOf(
            VersionedSource.sourceOnly(
               """

         @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL"
         )
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """
            ),
            VersionedSource.sourceOnly(CsvAnnotationSpec.taxi)
         ),
      )
      val csv = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL"""
      val result = TypedInstance.from(
         schema.type("Person[]"),
         csv,
         schema,
         formatSpecs = listOf(CsvFormatSpec)
      ) as TypedCollection
      result.toRawObject().should.equal(
         listOf(
            mapOf("firstName" to "jack", "lastName" to "jackery", "age" to 23),
            mapOf("firstName" to "jimmy", "lastName" to "smitts", "age" to null),
         )
      )
   }

   @Test
   fun `writes inherited properties`() {
      val schema = TaxiSchema.from("""
         model Person {
            id : Id inherits Int
            name : Name inherits String
         }
         @com.orbitalhq.formats.Csv
         model CsvPerson inherits Person
      """.trimIndent())
      val csv = """id,name
         |1,jack
         |2,jimmy""".trimMargin()
      val records = TypedInstance.from(
         schema.type("CsvPerson[]"),
         csv,
         schema,
         formatSpecs = listOf(CsvFormatSpec)
      ) as TypedCollection
      records.shouldHaveSize(2)
      (records[0] as TypedObject).get("name").toRawObject().shouldBe("jack")
      val back = records.toRawObject()
      back
   }


   @Test
   fun `uses defaults`() {
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.formats.Csv
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """.trimIndent()
      )
      val csv = """firstName,lastName,age
jack,jackery,23
jimmy,smitts,"""
      val result = TypedInstance.from(
         schema.type("Person[]"),
         csv,
         schema,
         formatSpecs = listOf(CsvFormatSpec)
      ) as TypedCollection
      result.toRawObject().should.equal(
         listOf(
            mapOf("firstName" to "jack", "lastName" to "jackery", "age" to 23),
            mapOf("firstName" to "jimmy", "lastName" to "smitts", "age" to null),
         )
      )
   }

   @Test
   fun `writes a single object to csv`() {
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.formats.Csv
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """.trimIndent()
      )
      val person = TypedInstance.from(
         schema.type("Person"),
         """{ "firstName" : "Jimmy", "lastName" : "jackery", "age": 23 }""",
         schema = schema
      )
      val (metadata, _) = FormatDetector(listOf(CsvFormatSpec)).getFormatType(schema.type("Person"))!!
      val generated = (CsvFormatSerializer.write(person, metadata, schema, 0) as String)
         .replace("\r\n", "\n")
      val expected = """firstName,lastName,age
Jimmy,jackery,23
"""
      generated.should.equal(expected)

      // now read it back
      val parsed = TypedInstance.from(
         schema.type("Person"), generated, schema,
         formatSpecs = listOf(CsvFormatSpec)
      )
      // Currently, the behaviour is to return a typed collection, regardless.
      // This is the existing behaviour, but it seems incorrect.
      // In future, we should change this so that:
      // - If T is requested, and it's a collection, throw an error
      // - If T is requested, and there's a single item, return T
      // - Otherwise, parse as T[]
      parsed.shouldBeInstanceOf<TypedObject>()
         .shouldBe(person)
   }

   @Test
   fun `can write using Csv spec`() {

      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.formats.Csv(
            delimiter = "|",
            nullValue = "NULL"
         )
         model Person {
            firstName : String by column("firstName")
            lastName : String by column("lastName")
            age : Int by column("age")
         }
      """.trimIndent()
      )
      val csv = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL"""
      val result = TypedInstance.from(
         schema.type("Person[]"),
         csv,
         schema,
         formatSpecs = listOf(CsvFormatSpec)
      ) as TypedCollection
      result.toRawObject().should.equal(
         listOf(
            mapOf("firstName" to "jack", "lastName" to "jackery", "age" to 23),
            mapOf("firstName" to "jimmy", "lastName" to "smitts", "age" to null),
         )
      )
      val (metadata, _) = FormatDetector(listOf(CsvFormatSpec)).getFormatType(schema.type("Person"))!!
      val generated = (CsvFormatSerializer.write(result, metadata, schema, -1) as String)
         .replace("\r\n", "\n")
      val expected = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL
"""
      generated.should.equal(expected)
   }

   @Test
   fun `can write normal object to csv when useFieldNamesAsColumnNames set to true`() {
      // This approach used when serializing a query result from an anonymous type
      val schema = TaxiSchema.from(
         """
         model Person {
            firstName : String
            lastName : String
            age : Int
         }
      """.trimIndent()
      )
      val typedCollection = TypedInstance.from(
         schema.type("Person[]"),
         """[
            { "firstName" : "jack" , "lastName" : "jackery", "age" : 23 },
            { "firstName" : "jimmy" , "lastName" : "smitts", "age" : null }
            ]
            """.trimMargin(),
         schema
      ) as TypedCollection
      val csvSpec = CsvFormatSpecAnnotation(
         delimiter = '|',
         nullValue = "NULL",
         useFieldNamesAsColumnNames = true
      )
      val generated = (CsvFormatSerializer.write(typedCollection, csvSpec, -1) as String)
         .replace("\r\n", "\n")
      val expected = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL
"""
      generated.should.equal(expected)
   }
}
