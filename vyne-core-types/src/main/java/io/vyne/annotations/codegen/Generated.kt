package io.vyne.annotations.codegen

import io.vyne.annotations.AnnotationWrapper
import lang.taxi.TaxiDocument
import lang.taxi.types.Annotation

class Generated : AnnotationWrapper {
   override fun asAnnotation(schema: TaxiDocument): Annotation {
      return Annotation(name = "io.vyne.codegen.Generated")
   }
}

/**
 * Annotation that indicates a file only contains a generated type.
 * Indication to code tooling that it's safe to delete the contents of the file
 * when regenerating the containing type.
 */
object SingleTypeInFile : AnnotationWrapper {
   override fun asAnnotation(schema:TaxiDocument): Annotation {
      return Annotation(name = "io.vyne.codegen.SingleTypeInFile")
   }
}
