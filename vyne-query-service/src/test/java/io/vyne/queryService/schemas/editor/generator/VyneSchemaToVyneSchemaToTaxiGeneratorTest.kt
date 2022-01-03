package io.vyne.queryService.schemas.editor.generator

import com.winterbe.expekt.should
import io.vyne.queryService.schemas.editor.EditedSchema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import lang.taxi.TaxiDocument
import lang.taxi.generators.GeneratedTaxiCode
import lang.taxi.testing.TestHelpers
import org.junit.Test

class VyneSchemaToVyneSchemaToTaxiGeneratorTest {

   @Test
   fun `generates types and models correctly`() {
      val source = """
         [[ Some docs for FirstName ]]
         @SomeAnnotation
         type Name inherits String

         type FirstName inherits Name
         type LastName inherits Name

         type PersonId inherits Int

         [[ Model Docs ]]
         model Person {
            @Id
            id : PersonId
            age : Int
            [[ Field docs ]]
            firstName : FirstName
            lastName : LastName
            country : Country
            friends : Person[]
         }
         [[ EnumDocs ]]
         @EnumAnnotation
         enum Country {
            NZ("New Zealand"),
            UK("United Kingdom")
         }

      """

      // Create a Vyne schema first...
      val schema = TaxiSchema.from(
         source
      ).toPartialSchema()
      // Then convert that into a taxi schema
      val schemaGenerator = VyneSchemaToTaxiGenerator()
      val generated = schemaGenerator.generate(schema)

      generated.shouldCompileTheSameAs(source)
   }

   @Test
   fun `types that are from another schema are imported not generated`() {
      val referenceSchema = TaxiSchema.from(
         """
         namespace io.vyne.demo

         type Name inherits String
         type FirstName inherits Name
         type LastName inherits Name
      """.trimIndent()
      )
      val taxi = """
           import io.vyne.demo.FirstName
           import io.vyne.demo.LastName

            namespace io.vyne.another

           model Person {
            firstName : FirstName
            lastName : LastName
         }
      """
      val schemaToGenerate = TaxiSchema.from(taxi, importSources = listOf(referenceSchema)).toPartialSchema()
      val schemaGenerator = VyneSchemaToTaxiGenerator()
      val generated = schemaGenerator.generate(schemaToGenerate, referenceSchema)
      // Can't use shouldCompileTheSameAs here, as it doesn't handle imports.
      // Key point in this is that firstName and lastName are not declared within the schema, as they're
      // present in the reference schema.
      val expected = """namespace io.vyne.another {
   model Person {
      firstName : io.vyne.demo.FirstName
      lastName : io.vyne.demo.LastName
   }
}"""
      generated.taxi[0].withoutWhitespace().should.equal(expected.withoutWhitespace())
   }
}

private fun TaxiSchema.toPartialSchema(): EditedSchema {
   return EditedSchema(
      this.types,
      this.services
   )
}

fun GeneratedTaxiCode.shouldCompileTheSameAs(expected: String): TaxiDocument {
   return TestHelpers.expectToCompileTheSame(this.taxi, expected)
}
