package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type

class BetweenTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val primitive = TemporalFieldUtils.validate(field)!!
      val startParameter = TemporalFieldUtils.parameterFor(primitive, TemporalFieldUtils.Start)
      val endParameter = TemporalFieldUtils.parameterFor(primitive, TemporalFieldUtils.End)
      val greaterThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start)
      val lessThanEqualConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.End)
      val returnType = ArrayType(type = type, source = CompilationUnit.unspecified())
      return Operation(
         name = "findBy${field.name.capitalize()}$ExpectedAnnotationName",
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(),
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
   }
}
