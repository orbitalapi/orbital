package io.vyne.cask.query.generators

import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.services.Operation
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Type
import org.springframework.stereotype.Component

/**
 * OperationGenerator to generate operation for stream all queries e.g.
 * stream { some.thing }
 */
@Component
class StreamAllGenerator : DefaultOperationGenerator {
   override
   fun generate(type: Type): Operation {

      return Operation(
         name = OperationAnnotation.StreamAll.annotation,
         parameters = emptyList(),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type)))),
         returnType = TemporalFieldUtils.streamTypeOf(type),
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }


    fun expectedAnnotationName(): OperationAnnotation {
      return OperationAnnotation.StreamAll
   }

   companion object {
      private fun getRestPath(type: Type): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}streamAll/${path.parts.joinToString("/")}"
      }
   }

}
