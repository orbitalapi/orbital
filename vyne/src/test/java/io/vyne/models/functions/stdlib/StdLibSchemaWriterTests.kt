package io.vyne.models.functions.stdlib

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.Compiler
import lang.taxi.generators.SchemaWriter
import org.junit.Test
import kotlin.test.fail

class StdLibSchemaWriterTests {

   @Test
   fun `outputs left statements`() {
      val generated = """
               type FirstName inherits String
               type FullName inherits String

               model Person {
                  firstName: FirstName
                  leftName : FullName by left(this.firstName, 5)
               }

            """.compileAndRegenerateSource()
      val expected = """type FirstName inherits lang.taxi.String

type FullName inherits lang.taxi.String

type Person {
   firstName : FirstName
   leftName : FullName by taxi.stdlib.left( this.firstName, 5)
}
"""
      generated.withoutWhitespace().should.equal(expected.withoutWhitespace())
      generated.shouldCompile()
   }
   @Test
   fun `outputs concatenated columns`() {
      val generated = """
         type Thing {
            primaryKey: String by concat(column(0), "-", column("NAME"), "-", column(2))
         }
      """.compileAndRegenerateSource()
      val expected = """
         type Thing {
           primaryKey : String  by taxi.stdlib.concat( column(0),"-",column("NAME"),"-",column(2) )
         }
      """
      generated.withoutWhitespace().should.equal(expected.withoutWhitespace())
      generated.shouldCompile()
   }

   @Test
   fun `outputs nested functions`() {
      val generated = """
         type Thing {
            primaryKey: String by upperCase(left("asdf",3))
         }
      """.compileAndRegenerateSource()
      val expected = """
         type Thing {
           primaryKey : String  by taxi.stdlib.upperCase(taxi.stdlib.left("asdf",3))
         }
      """
      generated.withoutWhitespace().should.equal(expected.withoutWhitespace())
      generated.shouldCompile()
   }
}


private fun String.shouldCompile() {
   val errors = Compiler.forStrings(this).validate()
   if (errors.isNotEmpty()) {
      fail("Expected source to compile, but found ${errors.size} compilation errors: \n ${errors.joinToString("\n") { it.detailMessage }}")
   }
}

private fun String.compileAndRegenerateSource(): String {
   // Use TaxiSchema to ensure stdlib gets baked in.
   val schema = TaxiSchema.from(this).taxi
   return SchemaWriter().generateSchemas(listOf(schema))[0]
}


fun String.withoutWhitespace(): String {
   return this
      .lines()
      .map { it.trim().replace(" ","") }
      .filter { it.isNotEmpty() }
      .joinToString("")
}
