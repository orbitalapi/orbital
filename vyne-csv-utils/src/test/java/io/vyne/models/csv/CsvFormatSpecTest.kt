package io.vyne.models.csv

import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.from
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.format.EmptyTypedInstanceInfo
import io.vyne.models.format.FormatDetector
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class CsvFormatSpecTest {

   @Test
   fun `can parse with pipe delimiter null value`() {

      val schema = TaxiSchema.from(
         listOf(
            VersionedSource.sourceOnly(
               """

         @io.vyne.formats.Csv(
            delimiter = "|",
            nullValue = "NULL"
         )
         model Person {
            firstName : String by column("firstName")
            lastName : String by column("lastName")
            age : Int by column("age")
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
         @io.vyne.formats.Csv
         model Person {
            firstName : String by column("firstName")
            lastName : String by column("lastName")
            age : Int by column("age")
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
         @io.vyne.formats.Csv(
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
      val generated = (CsvFormatSpec.serializer.write(result, metadata) as String)
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
      val generated = (CsvFormatSpec.serializer.write(typedCollection, csvSpec, EmptyTypedInstanceInfo) as String)
         .replace("\r\n", "\n")
      val expected = """firstName|lastName|age
jack|jackery|23
jimmy|smitts|NULL
"""
      generated.should.equal(expected)
   }
}
