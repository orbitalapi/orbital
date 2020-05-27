package io.vyne.cask.query.generators

import io.vyne.cask.query.CaskServiceSchemaGenerator
import io.vyne.cask.query.CaskServiceSchemaGenerator.Companion.operationReturnType
import io.vyne.cask.query.OperationGenerator
import lang.taxi.services.Operation
import lang.taxi.services.Parameter
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.PrimitiveType
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindByFieldIdOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameter = Parameter(
         annotations = listOf(Annotation("PathVariable", mapOf("name" to field.name))),
         type = field.type,
         name = field.name,
         constraints = listOf())

      return Operation(
         name = "findBy${field.name.capitalize()}",
         parameters = listOf(parameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getFindByIdRestPath(type, field)))),
         returnType = operationReturnType(type),
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return PrimitiveType.isAssignableToPrimitiveType(field.type)
   }

   private fun getFindByIdRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val fieldTypeQualifiedName = field.type.toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/{$fieldTypeQualifiedName}"
   }
}
