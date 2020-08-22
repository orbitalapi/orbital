package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.services.Operation
import lang.taxi.types.*
import lang.taxi.types.Annotation
import org.springframework.stereotype.Component

@Component
class FindAllGenerator: OperationGenerator {
   override fun generate(field: Field?, type: Type): Operation {
      return Operation(
         name = "findAll",
         parameters = emptyList(),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type)))),
         returnType = TemporalFieldUtils.collectionTypeOf(type),
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return false
   }

   override fun expectedAnnotationName(): OperationAnnotation {
      return OperationAnnotation.FindAll
   }

   companion object {
      private fun getRestPath(type: Type): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}findAll/${path.parts.joinToString("/")}"
      }
   }

}
