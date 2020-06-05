package io.vyne.cask.query.generators

import io.vyne.cask.query.CaskServiceSchemaGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterType
import io.vyne.schemas.asVyneTypeReference
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.ArrayType
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type
import lang.taxi.types.TypeAlias
import org.springframework.stereotype.Component

@Component
class BetweenTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val inheritedType = parameterType(field)
      val startParameter = TemporalFieldUtils.parameterFor(
         inheritedType,
         TemporalFieldUtils.Start,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Start))))
      val endParameter = TemporalFieldUtils.parameterFor(
         inheritedType,
         TemporalFieldUtils.End,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.End))))
      val greaterThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start)
      val lessThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.End)
      val returnType = ArrayType(type = type, source = CompilationUnit.unspecified())
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
