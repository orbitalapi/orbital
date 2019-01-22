package io.vyne.schemas.taxi

import com.winterbe.expekt.expect
import org.junit.Test

class TaxiSchemaTest {

   @Test
   fun when_creatingMultipleSchemas_then_importsAreRespected() {
      val srcA = """
namespace foo

type Person {
   name : FirstName as String
}""".trimIndent()

      val srcB: String = """
import foo.Person

namespace bar

type Book {
   author : foo.Person
}
      """.trimIndent()

      val srcC: String = """
import bar.Book

namespace baz

 type Library {
   inventory : bar.Book[]
}
      """.trimIndent()

      val schemas = TaxiSchema.from(NamedSource.unnamed(listOf(srcC, srcB, srcA)))
      expect(schemas).to.have.size(3)
   }

   @Test(expected = CircularDependencyInSourcesException::class)
   fun when_importingMultipeSources_that_circularDependenciesAreNotPermitted() {
      val srcA = """
import baz.Library
namespace foo

type Person {
   name : FirstName as String
    // this creates a circular dependency
   nearestLibrary: baz.Library
}""".trimIndent()

      val srcB: String = """
import foo.Person

namespace bar

type Book {
   author : foo.Person
}
      """.trimIndent()

      val srcC: String = """
import bar.Book

namespace baz

 type Library {
   inventory : bar.Book[]
}"""
      val schemas = TaxiSchema.from(NamedSource.unnamed(listOf(srcC, srcB, srcA)))
   }
}
