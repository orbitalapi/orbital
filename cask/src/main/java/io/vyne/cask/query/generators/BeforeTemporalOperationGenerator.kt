package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import io.vyne.cask.services.CaskServiceSchemaGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type

@Deprecated("Migrating to vyneQl endpoint")
//@Component
class BeforeTemporalOperationGenerator(val operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): OperationGenerator {
   override fun generate(field: Field?, type: Type): Operation {
      val parameterType = TemporalFieldUtils.parameterType(field!!)
      val beforeParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.Before,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Before))))
      val lessThanConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.Before)
      val returnType = collectionTypeOf(type)
      return Operation(
         name = "findBy${field.name.capitalize()}${expectedAnnotationName}",
         parameters = listOf(beforeParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(lessThanConstraint))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return TemporalFieldUtils.validate(field) != null &&
         (TemporalFieldUtils.annotationFor(field, expectedAnnotationName.annotation) != null ||
            operationGeneratorConfig.definesOperation(field.type, expectedAnnotationName))
   }

   fun expectedAnnotationName(): OperationAnnotation {
      return expectedAnnotationName
   }

   companion object {
      private val expectedAnnotationName = OperationAnnotation.Before

      private fun getRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$expectedAnnotationName/{before}"
      }
   }
}
