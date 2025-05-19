package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.Either
import com.orbitalhq.models.DeferredExpression
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object FilterEach : NamedFunctionInvoker, CollectionFilteringFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.FilterEach.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      rawMessageBeingParsed: Any?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      val inputValue = inputValues[0]
      val predicate = inputValues[1]
      if (predicate !is DeferredExpression) {
         return failed(returnType,function,inputValues,"Expected a predicate in position 1, but got ${predicate::class.simpleName}", predicate)
      }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val filterResult = evaluatePredicateAgainstMember(
         inputValue,
         schema,
         objectFactory,
         predicate,
         dataSource,
         returnType,
         function,
         inputValues
      )
      return when(filterResult) {
         is Either.Left -> filterResult.value
         is Either.Right -> {
            val expressionValue  = filterResult.value
            if (expressionValue) {
               inputValue
            } else {
               TypedNull.create(returnType, EvaluatedExpression(function.asTaxi(), inputValues))
            }
         }
      }
   }
}

