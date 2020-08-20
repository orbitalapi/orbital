package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.annotationFor
import io.vyne.cask.services.CaskServiceSchemaGenerator
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
class FindBySingleResultGenerator(val operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameter = Parameter(
         annotations = listOf(Annotation("PathVariable", mapOf("name" to field.name))),
         type = TemporalFieldUtils.parameterType(field),
         name = field.name,
         constraints = listOf())

      return Operation(
         name = "findOneBy${field.name.capitalize()}",
         parameters = listOf(parameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getFindByIdRestPath(type, field)))),
         returnType = type,
         compilationUnits = listOf(CompilationUnit.unspecified())
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return PrimitiveType.isAssignableToPrimitiveType(field.type) &&
         (annotationFor(field, expectedAnnotationName.annotation) != null ||
            operationGeneratorConfig.definesOperation(field.type, expectedAnnotationName))
   }

   private fun getFindByIdRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val fieldTypeQualifiedName =  TemporalFieldUtils.parameterType(field).toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return "${CaskServiceSchemaGenerator.CaskApiRootPath}findOneBy/${path.parts.joinToString("/")}/${field.name}/{$fieldTypeQualifiedName}"
   }

   companion object {
      private val expectedAnnotationName = OperationAnnotation.Association
   }
}
