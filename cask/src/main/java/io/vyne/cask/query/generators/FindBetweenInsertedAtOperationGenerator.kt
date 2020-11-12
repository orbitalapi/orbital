package io.vyne.cask.query.generators

import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.services.DefaultCaskTypeProvider
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.Annotation
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.Field
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindBetweenInsertedAtOperationGenerator(private val defaultCaskTypeProvider: DefaultCaskTypeProvider): DefaultOperationGenerator {

   protected val expectedAnnotationName = OperationAnnotation.Between

   override fun generate(type: Type): Operation {
      val insertedAtType = defaultCaskTypeProvider.insertedAtType()
      val startParameter = TemporalFieldUtils.parameterFor(
         insertedAtType,
         TemporalFieldUtils.Start,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.Start))))
      val endParameter = TemporalFieldUtils.parameterFor(
         insertedAtType,
         TemporalFieldUtils.End,
         listOf(Annotation("PathVariable", mapOf("name" to TemporalFieldUtils.End))))
      val (greaterThanEqualConstraint, lessThanConstraint) = constraints(insertedAtType)
      val returnType = TemporalFieldUtils.collectionTypeOf(type)
      return Operation(
         name = operationName(),
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to
            getRestPath(type)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanEqualConstraint, lessThanConstraint))
      )
   }

   protected fun restPath(typeQualifiedName: QualifiedName, path: AttributePath) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${fieldName}/$expectedAnnotationName/{start}/{end}"

   protected fun operationName() =  "findBy$fieldName$expectedAnnotationName"

   protected fun constraints(insertedAtType: Type): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN, TemporalFieldUtils.End))
   }

   private fun getRestPath(type: Type): String {
      val typeQualifiedName = type.toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return restPath(typeQualifiedName, path)
   }

   companion object {
      const val fieldName = "CaskInsertedAt"
   }
}

@Component
class InsertedAtGreaterThanStartLessThanEndOperationGenerator(defaultCaskTypeProvider: DefaultCaskTypeProvider):
   FindBetweenInsertedAtOperationGenerator(defaultCaskTypeProvider) {
   override fun constraints(insertedAtType: Type): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN, TemporalFieldUtils.End))
   }
   override fun operationName() = "${super.operationName()}${BetweenVariant.GtLt}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath): String =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/$fieldName/$expectedAnnotationName${BetweenVariant.GtLt}/{start}/{end}"
}

@Component
class InsertedAtGreaterThanStartLessThanOrEqualsToEndOperationGenerator(defaultCaskTypeProvider: DefaultCaskTypeProvider):
   FindBetweenInsertedAtOperationGenerator(defaultCaskTypeProvider) {
   override fun constraints(insertedAtType: Type): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN_OR_EQUAL_TO, TemporalFieldUtils.End))
   }
   override fun operationName() = "${super.operationName()}${BetweenVariant.GtLte}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath): String =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/$fieldName/$expectedAnnotationName${BetweenVariant.GtLte}/{start}/{end}"
}

@Component
class InsertedAtGreaterThanOrEqualsToStartLessThanOrEqualsToEndOperationGenerator(defaultCaskTypeProvider: DefaultCaskTypeProvider):
   FindBetweenInsertedAtOperationGenerator(defaultCaskTypeProvider) {
   override fun constraints(insertedAtType: Type): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN_OR_EQUAL_TO, TemporalFieldUtils.End))
   }
   override fun operationName() = "${super.operationName()}${BetweenVariant.GteLte}"
   override fun restPath(typeQualifiedName: QualifiedName, path: AttributePath): String =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/$fieldName/$expectedAnnotationName${BetweenVariant.GteLte}/{start}/{end}"
}
