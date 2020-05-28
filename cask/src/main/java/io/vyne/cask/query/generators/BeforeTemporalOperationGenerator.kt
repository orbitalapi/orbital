package io.vyne.cask.query.generators

import io.vyne.cask.query.OperationGenerator
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.ArrayType
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.Type

class BeforeTemporalOperationGenerator: OperationGenerator {
   override fun generate(field: Field, type: Type): Operation {
      val primitive = TemporalFieldUtils.validate(field)!!
      val beforeParameter = TemporalFieldUtils.parameterFor(primitive, TemporalFieldUtils.Before)
      val lessThanConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.Before)
      val returnType = ArrayType(type = type, source = CompilationUnit.unspecified())
      return Operation(
         name = "findBy${field.name.capitalize()}${ExpectedAnnotationName}",
         parameters = listOf(beforeParameter),
         annotations = listOf(),
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
   }

}
