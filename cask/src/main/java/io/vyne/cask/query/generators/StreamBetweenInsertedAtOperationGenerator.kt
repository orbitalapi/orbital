package io.vyne.cask.query.generators

import io.vyne.annotations.http.HttpOperations
import io.vyne.cask.query.DefaultOperationGenerator
import io.vyne.cask.services.CaskServiceSchemaGenerator
import io.vyne.cask.services.DefaultCaskTypeProvider
import lang.taxi.Operator
import lang.taxi.services.Operation
import lang.taxi.services.OperationContract
import lang.taxi.types.AttributePath
import lang.taxi.types.CompilationUnit
import lang.taxi.types.QualifiedName
import lang.taxi.types.Type
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

import org.springframework.stereotype.Component

/**
 * OperationGenerator to generate operations for cask inserted between / greater than /less than /equal to
 * dates e.g.
 *  stream { some.Thing ( CaskInsertedAt >=|>|<=|< "2021-05-06T00:00:00", CaskInsertedAt >=|>|<=|< "2021-05-07T00:00:00" ) }
 */
@Component
@ConditionalOnProperty("cask.streamQueries.enabled", havingValue = "true")
class StreamBetweenInsertedAtOperationGenerator(private val defaultCaskTypeProvider: DefaultCaskTypeProvider) : DefaultOperationGenerator {

   protected val expectedAnnotationName = OperationAnnotation.Between

   override
   fun generate(type: Type): Operation {
      val insertedAtType = defaultCaskTypeProvider.insertedAtType()
      val startParameter = TemporalFieldUtils.parameterFor(
         insertedAtType,
         TemporalFieldUtils.Start,
         listOf(HttpOperations.pathVariable(TemporalFieldUtils.Start))
      )
      val endParameter = TemporalFieldUtils.parameterFor(
         insertedAtType,
         TemporalFieldUtils.End,
         listOf(HttpOperations.pathVariable(TemporalFieldUtils.End))
      )
      val (greaterThanEqualConstraint, lessThanConstraint) = constraints(insertedAtType)
      val returnType = TemporalFieldUtils.streamTypeOf(defaultCaskTypeProvider.withDefaultCaskTaxiType(type))
      return Operation(
         name = streamOperationName(),
         parameters = listOf(startParameter, endParameter),
         annotations = listOf(HttpOperations.httpOperation(method = HttpOperations.HttpMethod.GET, url = getStreamPath(type))),
         returnType = returnType,
         compilationUnits = listOf(CompilationUnit.unspecified()),
         contract = OperationContract(
            returnType = returnType,
            returnTypeConstraints = listOf(greaterThanEqualConstraint, lessThanConstraint))
      )
   }

   protected fun streamPath(typeQualifiedName: QualifiedName, path: AttributePath) =
      "${CaskServiceSchemaGenerator.CaskApiRootPath}${path.parts.joinToString("/")}/${fieldName}/streamBetween/{start}/{end}"

   protected fun streamOperationName() = "streamBy$fieldName$expectedAnnotationName"

   protected fun constraints(insertedAtType: Type): BetweenConstraints {
      return BetweenConstraints(
         startConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.GREATER_THAN_OR_EQUAL_TO, TemporalFieldUtils.Start),
         endConstraint = TemporalFieldUtils.constraintFor(insertedAtType, Operator.LESS_THAN, TemporalFieldUtils.End))
   }

   private fun getStreamPath(type: Type): String {
      val typeQualifiedName = type.toQualifiedName()
      val path = AttributePath.from(typeQualifiedName.toString())
      return streamPath(typeQualifiedName, path)
   }

   companion object {
      const val fieldName = "CaskInsertedAt"
   }
}

