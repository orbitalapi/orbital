package io.vyne.schemas.taxi

import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.schemas.FieldModifier
import io.vyne.schemas.Modifier
import junit.framework.Assert.fail
import org.junit.Test

class TaxiSchemaTest {

   @Test
   fun when_creatingMultipleSchemas_then_importsAreRespected() {
      val srcA = """
namespace foo

type alias Age as Int
type Person {
   name : FirstName as String
}""".trimIndent()

      val srcB: String = """
import foo.Person

namespace bar

type alias PageNumber as Int
type Book {
   author : foo.Person
}
      """.trimIndent()

      val srcC: String = """
import bar.Book

namespace baz

type alias PhoneNumber as String
 type Library {
   inventory : bar.Book[]
}
      """.trimIndent()
      // This intentionally has no imports, to ensure it's still picked up correctl
      val srcD = """
namespace bak

type Video {}
      """.trimIndent()

      // Jumble the order of imported sources
      val schemas = TaxiSchema.from(NamedSource.unnamed(listOf(srcC, srcA, srcB, srcD)))
      expect(schemas).to.have.size(1)
      val schema = schemas.first()
      schema.type("baz.Library").attribute("inventory").type.fullyQualifiedName.should.equal("bar.Book")

      val missingTypes = listOf("baz.PhoneNumber", "baz.Library", "bar.PageNumber", "bar.Book", "foo.Age", "foo.Person", "bak.Video").mapNotNull {
         if (!schema.hasType(it)) { it } else null
      }
      if (missingTypes.isNotEmpty()) {
         fail("The following types are missing: ${missingTypes.joinToString(",")}")
      }
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

   @Test
   fun given_taxiTypeIsClosed_when_imported_then_vyneTypeShouldBeClosed() {

      val src = """
 closed type Money {
   currency : Currency as String
   value : MoneyValue as Int
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      expect(schema.type("Money").modifiers).to.contain(Modifier.CLOSED)
   }

   @Test
   fun given_taxiFieldIsClosed_when_imported_then_vyneFieldShouldBeClosed() {
      val src = """
 type Money {
   closed currency : Currency as String
   value : MoneyValue as Int
}
      """.trimIndent()
      val schema = TaxiSchema.from(src)
      expect(schema.type("Money").attribute("currency").modifiers).to.contain(FieldModifier.CLOSED)

   }

   @Test
   fun when_importingTypeExtensionsAcrossMutlipleFiles_then_theyAreApplied() {

      val srcA = """
namespace foo

type Customer {}""".trimIndent()

      val srcB = """
import foo.Customer

namespace bar

[[ I am docs ]]
type extension Customer {}
      """.trimIndent()
      val schemas = TaxiSchema.from(NamedSource.unnamed(listOf(srcB, srcA)))
      expect(schemas).to.have.size(1)
      val schema = schemas.first()
      schema.type("foo.Customer").typeDoc.should.equal("I am docs")

   }


}
