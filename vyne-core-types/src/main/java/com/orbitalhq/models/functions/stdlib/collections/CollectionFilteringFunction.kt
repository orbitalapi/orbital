package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.*
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType

abstract class CollectionFilteringFunction : NullSafeInvoker() {
   protected fun failed(
      returnType: Type,
      function: FunctionAccessor,
      inputValues: List<TypedInstance>,
      message: String,
      inputInError: TypedInstance? = null,
      cause: DataSource? = null,
   ): TypedNull {
      return TypedNull.create(
         returnType,
         FailedEvaluatedExpression(function.asTaxi(), inputValues, message, inputInError = inputInError, cause = cause)
      )
   }

   protected fun extractAndValidateInputs(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
   ): Either<TypedNull, Triple<TypedCollection, DeferredExpression, EvaluatedExpression>> {
      fun createTypeNullFailure(message: String): TypedNull {
         return createFailureWithTypedNull(
            message, returnType, function, inputValues
         )
      }

      val collection = when (val collection = inputValues[0]) {
         is TypedNull -> {
            return createTypeNullFailure("Expected a collection in param 0, but got null.").left()
         }

         is TypedCollection -> collection
         else -> error("Expected either a TypedNull or a TypedCollection in param 0.  Got a ${collection::class.simpleName}")
      }

      val deferredInstance = inputValues[1] as DeferredExpression
      val expressionReturnType = schema.type(deferredInstance.expression.returnType)

      if (expressionReturnType.basePrimitiveTypeName?.parameterizedName != PrimitiveType.BOOLEAN.qualifiedName) {
         return createTypeNullFailure("Expected a predicate that returned a boolean, but the returning type was ${expressionReturnType.qualifiedName.parameterizedName}")
            .left()
      }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      return Triple(collection, deferredInstance, dataSource).right()
   }

   protected suspend fun applyFilter(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): Either<TypedNull, List<TypedInstance>> {
      return when (val extracted = extractAndValidateInputs(inputValues, schema, returnType, function)) {
         is Either.Left -> extracted
         is Either.Right -> {
            val (collection, deferredExpression, dataSource) = extracted.value
            val filtered = mutableListOf<TypedInstance>()
            for (collectionMember in collection) {
               val filterResult = evaluatePredicateAgainstMember(
                  collectionMember,
                  schema,
                  objectFactory,
                  deferredExpression,
                  dataSource,
                  returnType,
                  function,
                  inputValues
               )
               when (filterResult) {
                  is Either.Left -> return filterResult.value.left()
                  is Either.Right -> if (filterResult.value) filtered.add(collectionMember)
               }
            }
            filtered.right()
         }
      }
   }

   protected suspend fun evaluatePredicateAgainstMember(
      collectionMember: TypedInstance,
      schema: Schema,
      objectFactory: EvaluationValueSupplier,
      deferredInstance: DeferredExpression,
      dataSource: EvaluatedExpression,
      returnType: Type,
      function: FunctionAccessor,
      inputValues: List<TypedInstance>
   ): Either<TypedNull, Boolean> {
      val factBag = FactBagValueSupplier.of(listOf(collectionMember), schema, thisScopeValueSupplier = objectFactory)
      val evaluated = deferredInstance.evaluate(collectionMember, dataSource, factBag)

      if (evaluated.type.basePrimitiveTypeName?.parameterizedName != PrimitiveType.BOOLEAN.qualifiedName) {
         return failed(
            returnType,
            function,
            inputValues,
            "After evaluating the predicate (${deferredInstance.expression.asTaxi()}), expected a return type of boolean, but the returned instance had type ${evaluated.type.qualifiedName.parameterizedName}",
            evaluated
         ).left()
      }
      if (evaluated is TypedNull) {
         return failed(
            returnType,
            function,
            inputValues,
            "When evaluating the predicate  (${deferredInstance.expression.asTaxi()}), a null value was returned, which cannot be cast to boolean",
            evaluated,
            evaluated.source
         ).left()
      }
      return (evaluated.value as Boolean).right()
   }
}

fun createFailureWithTypedNull(
   message: String,
   returnType: Type,
   function: FunctionAccessor,
   inputValues: List<TypedInstance>
): TypedNull {
   return TypedNull.create(
      returnType,
      FailedEvaluatedExpression(
         function.asTaxi(),
         inputValues,
         message
      )
   )
}
