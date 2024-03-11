package com.orbitalhq.cockpit.core.schemas.editor.operations

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.schemas.editor.SchemaSubmissionResult
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddOrRemoveFieldAnnotationTest : BaseSchemaEditOperationTest() {

   @Test
   fun `can add an annotation to a field`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {
   filmId : FilmId inherits String
   title : Title inherits String
}
""",
         AddOrRemoveFieldAnnotation(
            symbol = "com.films.Film".fqn(),
            annotationName = "Id",
            operation = AddOrRemoveFieldAnnotation.Operation.Add,
            fieldName = "filmId"
         )
      )
      val type = result.types.single { it.name.fullyQualifiedName == "com.films.Film" }
      val annotation = type.attribute("filmId").metadata.shouldHaveSize(1).single()
      annotation.name.fullyQualifiedName.shouldBe("Id")
   }

   @Test
   fun `can add an annotation to a field that has existing annotations`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {
   @Foo(bar = "baz" )
   filmId : FilmId inherits String
   title : Title inherits String
}
""",
         AddOrRemoveFieldAnnotation(
            symbol = "com.films.Film".fqn(),
            annotationName = "Id",
            operation = AddOrRemoveFieldAnnotation.Operation.Add,
            fieldName = "filmId"
         )
      )
      val type = result.types.single { it.name.fullyQualifiedName == "com.films.Film" }
      val metadata = type.attribute("filmId").metadata
      metadata.shouldHaveSize(2)
         .shouldHaveSingleElement { it -> it.name.fullyQualifiedName == "Id" }
      metadata.shouldHaveSingleElement { it -> it.name.fullyQualifiedName == "Foo" }
   }

   @Test
   fun `can remove an annotation where annotation is on single line`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {
   @Id
   @Foo(bar = "baz" )
   filmId : FilmId inherits String
   title : Title inherits String
}
""",
         AddOrRemoveFieldAnnotation(
            symbol = "com.films.Film".fqn(),
            annotationName = "Foo",
            operation = AddOrRemoveFieldAnnotation.Operation.Remove,
            fieldName = "filmId"
         )
      )
      val type = result.types.single { it.name.fullyQualifiedName == "com.films.Film" }
      val metadata = type.attribute("filmId").metadata
      metadata.shouldHaveSize(1)
         .shouldHaveSingleElement { it -> it.name.fullyQualifiedName == "Id" }
   }

   @Test
   fun `can remove an annotation where annotation is on same line`() {
      stubPackageApiToReturnEmptyPackage()
      val result = applyEdit(
         "Films.taxi",
         """
namespace com.films

model Film {
   @Id @Foo(bar = "baz" ) @SomethingElse
   filmId : FilmId inherits String
   title : Title inherits String
}
""",
         AddOrRemoveFieldAnnotation(
            symbol = "com.films.Film".fqn(),
            annotationName = "Foo",
            operation = AddOrRemoveFieldAnnotation.Operation.Remove,
            fieldName = "filmId"
         )
      )
      val type = result.types.single { it.name.fullyQualifiedName == "com.films.Film" }
      val metadata = type.attribute("filmId").metadata
      metadata.shouldHaveSize(2)
         .shouldHaveSingleElement { it -> it.name.fullyQualifiedName == "Id" }
      metadata.shouldHaveSingleElement { it -> it.name.fullyQualifiedName == "SomethingElse" }
   }



}
