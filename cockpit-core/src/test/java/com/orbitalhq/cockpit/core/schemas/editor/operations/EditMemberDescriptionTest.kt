package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.SchemaMemberKind
import com.orbitalhq.schemas.fqn
import com.orbitalhq.utils.asA
import io.kotest.matchers.shouldBe
import lang.taxi.types.ObjectType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EditMemberDescriptionTest : BaseSchemaEditOperationTest() {

   @Test
   fun `can add description to a model`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.TYPE,
            symbol = "com.films.Film".fqn(),
            memberName = null,
            typeDoc = "Hello, world"
         )
      )
      result.type("com.films.Film".fqn())
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can change description to a model`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

[[ Hello, world ]]
model Film {}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.TYPE,
            symbol = "com.films.Film".fqn(),
            memberName = null,
            typeDoc = "Goodbye, world"
         )
      )
      result.type("com.films.Film".fqn())
         .typeDoc.shouldBe("Goodbye, world")
   }

   @Test
   fun `can add description to a model field`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {

   title : FilmTitle inherits String
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.TYPE,
            symbol = "com.films.Film".fqn(),
            memberName = "title",
            typeDoc = "Hello, world"
         )
      )
      result.type("com.films.Film".fqn())
         .attributes["title"]!!
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can update description to a model field`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {

   [[ Hello, world ]]
   title : FilmTitle inherits String
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.TYPE,
            symbol = "com.films.Film".fqn(),
            memberName = "title",
            typeDoc = "Goodbye, world"
         )
      )
      result.type("com.films.Film".fqn())
         .attributes["title"]!!
         .typeDoc.shouldBe("Goodbye, world")
   }

   @Test
   fun `can add description to a service`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

service Movies {
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.SERVICE,
            symbol = "com.films.Movies".fqn(),
            typeDoc = "Hello, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can update description to a service`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

[[ Hello, world ]]
service Movies {
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.SERVICE,
            symbol = "com.films.Movies".fqn(),
            typeDoc = "Goodbye, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .typeDoc.shouldBe("Goodbye, world")
   }


   @Test
   fun `can add description to an api operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

service Movies {
   operation listMovies()
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "listMovies"),
            typeDoc = "Hello, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .operations.single()
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can update description to an api operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

service Movies {
   [[ Hello, world ]]
   operation listMovies()
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "listMovies"),
            typeDoc = "Goodbye, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .operations.single()
         .typeDoc.shouldBe("Goodbye, world")
   }


   @Test
   fun `can add description to a table operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film

service Movies {
   table films : Film[]
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "films"),
            typeDoc = "Hello, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .tableOperations.single()
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can update description to a table operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film

service Movies {
   [[ Hello, world ]]
   table films : Film[]
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "films"),
            typeDoc = "Goodbye, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .tableOperations.single()
         .typeDoc.shouldBe("Goodbye, world")
   }

   @Test
   fun `can add description to a strean operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film

service Movies {
   stream films : Stream<Film>
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "films"),
            typeDoc = "Hello, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .streamOperations.single()
         .typeDoc.shouldBe("Hello, world")
   }

   @Test
   fun `can update description to a stream operation`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film

service Movies {
   [[ Hello, world ]]
   stream films : Stream<Film>
}
         """.trimIndent(),
         EditMemberDescription(
            SchemaMemberKind.OPERATION,
            symbol = OperationNames.qualifiedName("com.films.Movies", "films"),
            typeDoc = "Goodbye, world"
         )
      )
      result.service("com.films.Movies".fqn())
         .streamOperations.single()
         .typeDoc.shouldBe("Goodbye, world")
   }
}
