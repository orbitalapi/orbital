package com.orbitalhq.formats.csv

import com.winterbe.expekt.should
import com.orbitalhq.VersionedSource
import com.orbitalhq.from
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.EmptyTypedInstanceInfo
import com.orbitalhq.models.format.FormatDetector
import com.orbitalhq.schemas.taxi.TaxiSchema
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
      val generated = (CsvFormatSerializer.write(result, metadata) as String)
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
      val generated = (CsvFormatSerializer.write(typedCollection, csvSpec, EmptyTypedInstanceInfo) as String)
         .replace("\r\n", "\n")
      val expected = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL
"""
      generated.should.equal(expected)
   }
}
