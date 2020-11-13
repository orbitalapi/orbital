package io.vyne.cask.query.generators

import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.query.OperationGenerator
import io.vyne.cask.query.generators.TemporalFieldUtils.collectionTypeOf
import io.vyne.cask.query.generators.TemporalFieldUtils.parameterType
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.services.operations.constraints.PropertyToParameterConstraint
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import org.springframework.stereotype.Component

// For Temporal fields, it creates the following operation:
//  @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
//  operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime )
//  : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime < end )
//
@Component
class BetweenTemporalOperationGenerator(val operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()) : OperationGenerator {
   protected val expectedAnnotationName = OperationAnnotation.Between
   override fun generate(field: Field?, type: Type): Operation {
      val parameterType = parameterType(field!!)
      val startParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.Start,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Start))))
      val endParameter = TemporalFieldUtils.parameterFor(
         parameterType,
         TemporalFieldUtils.End,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.End))))
      val (greaterThanEqualConstraint, lessThanEqualConstraint) = constraints(field)
      val returnType = collectionTypeOf(type)
      return Operation(
         name = operationName(field),
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to getRestPath(type, field)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanEqualConstraint, lessThanEqualConstraint))
      )
   }

   private fun getRestPath(type: Type, field: Field): String {
      val typeQualifiedName = type.toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return restPath(typeQualifiedName, path, field)
   }

   protected fun restPath(typeQualifiedName: QualifiedName, path: AttributePath, field: Field) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$expectedAnnotationName/{start}/{end}"

   protected fun operationName(field: Field) = "findBy${field.name.capitalize()}$expectedAnnotationName"

   protected fun constraints(field: Field): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.End))
   }

   override fun canGenerate(field: Field, type: Type): Boolean {
      return TemporalFieldUtils.validate(field) != null &&
         (TemporalFieldUtils.annotationFor(field, expectedAnnotationName.annotation) != null ||
            operationGeneratorConfig.definesOperation(field.type, expectedAnnotationName))
   }

   override fun expectedAnnotationName(): OperationAnnotation {
      return expectedAnnotationName
   }
}

// For Temporal fields, it creates the following operation:
//  @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
//  operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime )
//  : OrderWindowSummary[]( TransactionEventDateTime > start, TransactionEventDateTime < end )
//
@Component
class GreaterThanStartLessThanEndOperationGenerator(operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): BetweenTemporalOperationGenerator(operationGeneratorConfig) {
   override fun constraints(field: Field): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN, TemporalFieldUtils.End))
   }

   override fun operationName(field: Field) = "findBy${field.name.capitalize()}$expectedAnnotationName${BetweenVariant.GtLt}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath, field: Field) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$expectedAnnotationName${BetweenVariant.GtLt}/{start}/{end}"
}

// For Temporal fields, it creates the following operation:
//  @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
//  operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime )
//  : OrderWindowSummary[]( TransactionEventDateTime > start, TransactionEventDateTime <= end )
//
@Component
class GreaterThanStartLessThanOrEqualsToEndOperationGenerator(operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): BetweenTemporalOperationGenerator(operationGeneratorConfig) {
   override fun constraints(field: Field): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN_OR_EQUAL_TO, TemporalFieldUtils.End))
   }
   override fun operationName(field: Field) = "findBy${field.name.capitalize()}$expectedAnnotationName${BetweenVariant.GtLte}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath, field: Field) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$expectedAnnotationName${BetweenVariant.GtLte}/{start}/{end}"
}

// For Temporal fields, it creates the following operation:
//  @HttpOperation(method = "GET" , url = "/api/cask/OrderWindowSummary/orderDateTime/Between/{start}/{end}")
//  operation findByOrderDateTimeBetween( @PathVariable(name = "start") start : TransactionEventDateTime, @PathVariable(name = "end") end : TransactionEventDateTime )
//  : OrderWindowSummary[]( TransactionEventDateTime >= start, TransactionEventDateTime <= end )
//
@Component
class GreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator(operationGeneratorConfig: OperationGeneratorConfig = OperationGeneratorConfig.empty()): BetweenTemporalOperationGenerator(operationGeneratorConfig) {
   override fun constraints(field: Field): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(field, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(field, Operator.LESS_THAN_OR_EQUAL_TO, TemporalFieldUtils.End))
   }
   override fun operationName(field: Field) = "findBy${field.name.capitalize()}$expectedAnnotationName${BetweenVariant.GteLte}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath, field: Field) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${field.name}/$expectedAnnotationName${BetweenVariant.GteLte}/{start}/{end}"
}

enum class BetweenVariant {
   GtLt,
   GtLte,
   GteLte
}
data class BetweenConstraints(val startConstraint: PropertyToParameterConstraint, val endConstraint: PropertyToParameterConstraint)
