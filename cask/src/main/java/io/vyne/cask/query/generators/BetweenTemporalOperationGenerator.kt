package io.vyne.cask.query.generators

import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterType
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class BetweenTemporalOperationGenerator : OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val parameterType = parameterType(field)
      val startParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.Start,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Start))))
      val endParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.End,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.End))))
      val greaterThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start)
      val lessThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.End)
      val returnType = collectionTypeOf(type)
      return Operation(
         name = "findBy${field.name.capitalize()}$ExpectedAnnotationName",
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanEqualConstraint, lessThanEqualConstraint))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return TemporalFieldUtils.validate(field) != null && TemporalFieldUtils.annotationFor(field, ExpectedAnnotationName) != null
   }

   companion object {
      const val ExpectedAnnotationName = "Between"
      private fun getRestPath(type: Type, field: Field): String {
         val typeQualifiedName = type.toQualifiedName()
         val fieldTypeQualifiedName = field.type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$ExpectedAnnotationName/{start}/{end}"
      }
   }
}
