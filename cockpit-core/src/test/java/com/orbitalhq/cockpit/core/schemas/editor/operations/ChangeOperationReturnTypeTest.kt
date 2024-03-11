package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChangeOperationReturnTypeTest : BaseSchemaEditOperationTest() {

   @Test
   fun `can change an API operation return type`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {}
model Actor {}

service Films {
   operation getThings():Film
}
         """.trimIndent(),
         ChangeOperationReturnType(
            OperationNames.qualifiedName("com.films.Films", "getThings"),
            "com.films.Actor".fqn()
         )
      )
      result.services.single().operation("getThings")
         .returnTypeName.fullyQualifiedName.shouldBe("com.films.Actor")
   }


   @Test
   fun `can change a table operation return type`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {}
model Actor {}

service Films {
   table things : Film[]
}
         """.trimIndent(),
         ChangeOperationReturnType(
            OperationNames.qualifiedName("com.films.Films", "things"),
            "com.films.Actor[]".fqn()
         )
      )
      result.services.single().tableOperations.single { it.name == "things" }
         .returnTypeName.parameterizedName.shouldBe("lang.taxi.Array<com.films.Actor>")
   }

   @Test
   fun `can change a stream operation return type`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {}
model Actor {}

service Films {
   stream things : Stream<Film>
}
         """.trimIndent(),
         ChangeOperationReturnType(
            OperationNames.qualifiedName("com.films.Films", "things"),
            "lang.taxi.Stream<Actor>".fqn()
         )
      )
      result.services.single().streamOperations.single { it.name == "things" }
         .returnTypeName.parameterizedName.shouldBe("lang.taxi.Stream<com.films.Actor>")
   }
}
