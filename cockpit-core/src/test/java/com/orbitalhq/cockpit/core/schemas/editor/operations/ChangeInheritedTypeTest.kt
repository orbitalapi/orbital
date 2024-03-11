package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.types.PrimitiveType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChangeInheritedTypeTest  : BaseSchemaEditOperationTest() {
   @Test
   fun `can replace a types base type`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

type Name inherits String
type FirstName inherits String
type PersonName inherits Name
         """.trimIndent(),
         ChangeInheritedType(
            symbol = "com.films.PersonName".fqn(),
            newBaseType = "com.films.FirstName".fqn()
         )
      )
      result.type("com.films.PersonName".fqn())
         .inheritsFromTypeNames
         .shouldContainExactlyInAnyOrder("com.films.FirstName".fqn())
   }

   @Test
   fun `can add a base type`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

type Name inherits String
type FirstName inherits String
type PersonName
         """.trimIndent(),
         ChangeInheritedType(
            symbol = "com.films.PersonName".fqn(),
            newBaseType = "com.films.Name".fqn()
         )
      )
      result.type("com.films.PersonName".fqn())
         .inheritsFromTypeNames
         .shouldContainExactlyInAnyOrder("com.films.Name".fqn())
   }
}
