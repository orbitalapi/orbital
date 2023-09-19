package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.models.*
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType

open class CollectionFilteringFunction {
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

   protected fun createFailureWithTypedNull(
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

   protected fun applyFilter(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): Either<TypedNull, List<TypedInstance>> {
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

      val filtered = collection.filter { collectionMember ->
         val factBag = FactBagValueSupplier.of(listOf(collectionMember), schema, thisScopeValueSupplier = objectFactory)
         val evaluated = deferredInstance.evaluate(collectionMember, dataSource, factBag)
//         val reader = AccessorReader(factBag, schema.functionRegistry, schema)
//         val evaluated = reader.evaluate(
//            collectionMember,
//            expressionReturnType,
//            deferredInstance.expression,
//            dataSource = dataSource,
//            format = null
//         )

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
         evaluated.value as Boolean
      }
      return filtered.right()
   }
}
