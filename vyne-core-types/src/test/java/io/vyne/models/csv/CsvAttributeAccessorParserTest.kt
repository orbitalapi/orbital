package io.vyne.models.csv

import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import io.vyne.models.*
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.accessors.ColumnAccessor
import org.apache.commons.csv.CSVFormat
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class CsvAttributeAccessorParserTest {


   @Test
   fun `when a model has column defined can still parse from a map`() {
      val schema = TaxiSchema.from(
         """
         model Movie {
            movieTitle : MovieTitle inherits String by column("title")
            rating : MovieRating inherits String by column("rating")
         }
      """.trimIndent()
      )
      val map = mapOf("movieTitle" to "Star Wars", "rating" to "G")
      val instance = TypedInstance.from(schema.type("Movie"), map, schema)
         as TypedObject
      instance.toRawObject().shouldBe(map)

   }

   @Test
   fun `When there is a parsing error for a nullable field its value is set to null`() {
      val schema = TaxiSchema.from(
         """
         enum Country {
            UK("United Kingdom"),
            DE("Germany")
         }
         model Foo {
            country : Country? by column("country")
         }
      """.trimIndent())


      val typeCountry = schema.type("Country")
      val appendable = StringWriter()
      val printer =  CSVFormat.DEFAULT.withHeader("country").print(appendable)
      printer.print("France")
      printer.flush()

      val targetCsvRecord = CSVFormat.DEFAULT.withHeader("country")
         .parse(StringReader(appendable.toString())).records
         .last()


      val parser = CsvAttributeAccessorParser()
      val result = parser.parseToType(
          typeCountry,
          ColumnAccessor("country", typeCountry.taxiType),
          targetCsvRecord,
          schema,
          emptySet(),
          Provided,
          true,
          null
      )

      result.should.equal(TypedNull.create(typeCountry, UndefinedSource))
      result.source.should.be.instanceof(FailedParsingSource::class.java)
      (result.source as FailedParsingSource).value.should.equal("France")
   }
}
