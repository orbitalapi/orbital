package io.vyne.cask.query.generators

import io.vyne.cask.query.CaskServiceSchemaGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.annotationFor
import io.vyne.cask.query.generators.TemporalFieldUtils.constraintFor
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterFor
import io.vyne.cask.query.generators.TemporalFieldUtils.validate
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type

class AfterTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val primitive = TemporalFieldUtils.validate(field)!!
      val afterParameter = parameterFor(primitive, TemporalFieldUtils.After)
      val greaterThanConstraint = constraintFor(field, Operator.GREATER_THAN, TemporalFieldUtils.After)
      return Operation(
         name = "findBy${field.name.capitalize()}${ExpectedAnnotationName}",
         parameters = listOf(afterParameter),
         annotations = listOf(),
         returnType = CaskServiceSchemaGenerator.operationReturnType(type),
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = CaskServiceSchemaGenerator.operationReturnType(type),
            returnTypeConstraints = listOf(greaterThanConstraint))
      )
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return validate(field) != null && annotationFor(field, ExpectedAnnotationName) != null
   }

   companion object {
      const val ExpectedAnnotationName = "After"
   }
}
