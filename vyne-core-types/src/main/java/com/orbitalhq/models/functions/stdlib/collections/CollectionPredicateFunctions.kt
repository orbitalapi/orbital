package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import com.orbitalhq.models.DeferredExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.functions.stdlib.All
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName


enum class CollectionOperationType(
   val valueIfReturningEarly: Boolean,
   val terminateWhenEvaluatesAs: Boolean,
   val valueIfAllEvaluated: Boolean
) {
   ALL(valueIfReturningEarly = false, terminateWhenEvaluatesAs = false, valueIfAllEvaluated = true),
   ANY(valueIfReturningEarly = true, terminateWhenEvaluatesAs = true, valueIfAllEvaluated = false),
   NONE(valueIfReturningEarly = false, terminateWhenEvaluatesAs = true, valueIfAllEvaluated = true),
}

abstract class BaseCollectionPredicateInvoker(val operationType: CollectionOperationType) :
   CollectionFilteringFunction(), NamedFunctionInvoker {
   override suspend fun doInvoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      rawMessageBeingParsed: Any?,
      thisScopeValueSupplier: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      resultCache: MutableMap<FunctionResultCacheKey, Any>
   ): TypedInstance {
      return extractAndValidateInputs(inputValues, schema, returnType, function)
         .map { (collection, deferredExpression, dataSource) ->
            collection.map { instance ->
               val eval = evaluatePredicateAgainstMember(
                  instance,
                  schema,
                  thisScopeValueSupplier,
                  deferredExpression,
                  dataSource,
                  returnType,
                  function,
                  inputValues
               )
               when (eval) {
                  // If the evaluation returned a typedNull, it indicates it failed, so
                  // bail out of the evaluation, returning at the top level
                  is Either.Left -> return eval.value
                  is Either.Right -> {
                     if (eval.value == operationType.terminateWhenEvaluatesAs) {
                        return TypedInstance.from(
                           returnType,
                           operationType.valueIfReturningEarly,
                           schema,
                           source = dataSource
                        )
                     } else {
                        eval.value
                     }
                  }
               }
            }
            TypedInstance.from(returnType, operationType.valueIfAllEvaluated, schema, source = dataSource)
         }.getOrHandle { it }
   }
}

object All : BaseCollectionPredicateInvoker(CollectionOperationType.ALL), NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.All.name
}

// Not named "Any" to avoid clashing with the Kotlin class
object AnyMatch : BaseCollectionPredicateInvoker(CollectionOperationType.ANY), NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Any.name
}

object None : BaseCollectionPredicateInvoker(CollectionOperationType.NONE), NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.None.name
}
