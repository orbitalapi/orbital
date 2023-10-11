package com.orbitalhq.models.functions.stdlib

import com.winterbe.expekt.should
import com.orbitalhq.schemas.taxi.TaxiSchema
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
      val expected = """type FirstName inherits String

type FullName inherits String

model Person {
   firstName : FirstName
   leftName : FullName by left( this.firstName, 5)
}
"""
      generated.withoutWhitespace().should.equal(expected.withoutWhitespace())
      generated.shouldCompile()
   }
   @Test
   fun `outputs concatenated columns`() {
      val generated = """
         type PrimarKey inherits String
         type Thing {
            primaryKey: PrimarKey by concat(column(0), "-", column("NAME"), "-", column(2))
         }
      """.compileAndRegenerateSource()
      val expected = """
         type PrimarKey inherits String
         model Thing {
           primaryKey :  PrimarKey by concat( column(0),"-",column("NAME"),"-",column(2) )
         }
      """

      generated.withoutWhitespace().should.equal(expected.withoutWhitespace())
      generated.shouldCompile()
   }

   @Test
   fun `outputs nested functions`() {
      val generated = """
         type PrimarKey inherits String
         type Thing {
            primaryKey: PrimarKey by upperCase(left("asdf",3))
         }
      """.compileAndRegenerateSource()
      val expected = """
         type PrimarKey inherits String
         model Thing {
           primaryKey : PrimarKey  by upperCase(left("asdf",3))
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
