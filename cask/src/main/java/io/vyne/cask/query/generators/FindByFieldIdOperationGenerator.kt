package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterType
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.services.Operation
import lang.taxi.services.Parameter
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindByFieldIdOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameter = Parameter(
         annotations = listOf(Annotation("PathVariable", mapOf("name" to field.name))),
         type = parameterType(field),
         name = field.name,
         constraints = listOf())

      val returnType = collectionTypeOf(type)
      return Operation(
         name = "findBy${field.name.capitalize()}",
         parameters = listOf(parameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getFindByIdRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      //return PrimitiveType.isAssignableToPrimitiveType(field.type)
      // Disable default generation for all simple fields for the moment.
      // We might want to revisit this in the future.
      return false
   }

   private fun getFindByIdRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val fieldTypeQualifiedName =  parameterType(field).toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/{$fieldTypeQualifiedName}"
   }
}
