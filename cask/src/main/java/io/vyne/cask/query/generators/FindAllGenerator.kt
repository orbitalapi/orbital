package io.vyne.cask.query.generators

import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.services.Operation
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindAllGenerator: DefaultOperationGenerator {
   override fun generate(type: Type): Operation {

      return Operation(
         name = "getAll",
         parameters = emptyList(),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type)))),
         returnType = TemporalFieldUtils.collectionTypeOf(type),
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }


    fun expectedAnnotationName(): OperationAnnotation {
      return OperationAnnotation.GetAll
   }

   companion object {
      private fun getRestPath(type: Type): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}findAll/${path.parts.joinToString("/")}"
      }
   }

}