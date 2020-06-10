package io.vyne.cask.query.generators

import io.vyne.cask.query.CaskServiceSchemaGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class BeforeTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameterType = TemporalFieldUtils.parameterType(field)
      val beforeParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.Before,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Before))))
      val lessThanConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.Before)
      val returnType = collectionTypeOf(type)
      return Operation(
         name = "findBy${field.name.capitalize()}${ExpectedAnnotationName}",
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
      return TemporalFieldUtils.validate(field) != null && TemporalFieldUtils.annotationFor(field, ExpectedAnnotationName) != null
   }

   companion object {
      const val ExpectedAnnotationName = "Before"
      private fun getRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$ExpectedAnnotationName/{before}"
      }
   }

}
