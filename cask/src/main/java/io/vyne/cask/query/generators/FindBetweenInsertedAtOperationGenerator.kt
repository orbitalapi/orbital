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
import lang.taxi.types.Type
import org.springframework.stereotype.Component

@Component
class FindBetweenInsertedAtOperationGenerator(private val defaultCaskTypeProvider: DefaultCaskTypeProvider): DefaultOperationGenerator {
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
      val greaterThanEqualConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start)
      val lessThanEqualConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN, TemporalFieldUtils.End)
      val returnType = TemporalFieldUtils.collectionTypeOf(type)
      return Operation(
         name = "findBy$fieldName$expectedAnnotationName",
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(Annotation("HttpOperation", mapOf("method" to "GET", "url" to
            getRestPath(type)))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanEqualConstraint, lessThanEqualConstraint))
      )
   }

   companion object {
      const val fieldName = "CaskInsertedAt"
      private val expectedAnnotationName = OperationAnnotation.Between

      private fun getRestPath(type: Type): String {
         val typeQualifiedName = type.toQualifiedName()
         val path = AttributePath.from(typeQualifiedName.toString())
         return "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/$fieldName/$expectedAnnotationName/{start}/{end}"
      }
   }
}
