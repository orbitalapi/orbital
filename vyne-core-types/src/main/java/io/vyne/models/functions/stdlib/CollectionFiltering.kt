package io.vyne.models.functions.stdlib

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import io.vyne.models.*
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object CollectionFiltering {
   val functions: List<NamedFunctionInvoker> = listOf(
      Single,
      FilterAll,
      ArrayEquals
   )
}

object Single : NamedFunctionInvoker, CollectionFilteringFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Single.name

   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): TypedInstance {
      return applyFilter(inputValues, schema, returnType, function, objectFactory, rawMessageBeingParsed)
         .map { filtered ->
            when {
               filtered.isEmpty() -> failed(
                  returnType,
                  function,
                  inputValues,
                  "After filtering, zero matching elements were found"
               )

               filtered.size > 1 -> failed(
                  returnType,
                  function,
                  inputValues,
                  "After filtering, expected exactly one matching element, but ${filtered.size} were found"
               )

               else -> filtered.single()
            }
         }.getOrHandle { it }
   }
}

object FilterAll : NamedFunctionInvoker, CollectionFilteringFunction()  {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.FilterAll.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): TypedInstance {
      return applyFilter(inputValues, schema, returnType, function, objectFactory, rawMessageBeingParsed)
         .map {
            if (it.isEmpty()) {
               TypedCollection.empty(returnType)
            } else {
               TypedCollection.from(it, source = EvaluatedExpression(function.asTaxi(), inputValues)) }
            }

         .getOrHandle { it }
   }
}

object ArrayEquals : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.ArrayEquals.name
   override fun invoke(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): TypedInstance {
      TODO("Not yet implemented")
   }
}

open class CollectionFilteringFunction {
   protected fun failed(
      returnType: Type,
      function: FunctionAccessor,
      inputValues: List<TypedInstance>,
      message: String,
      inputInError: TypedInstance? = null,
      cause: DataSource? = null,
   ): TypedNull {
      return TypedNull.create(returnType, FailedEvaluatedExpression(function.asTaxi(), inputValues, message, inputInError = inputInError, cause = cause))
   }

   protected fun applyFilter(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessageBeingParsed: Any?
   ): Either<TypedNull, List<TypedInstance>> {
      val collection = inputValues[0] as TypedCollection
      val deferredInstance = inputValues[1] as DeferredTypedInstance
      val expressionReturnType = schema.type(deferredInstance.expression.returnType)

      if (expressionReturnType.basePrimitiveTypeName?.parameterizedName != PrimitiveType.BOOLEAN.qualifiedName) {
         return TypedNull.create(
            returnType,
            FailedEvaluatedExpression(
               function.asTaxi(),
               inputValues,
               "Expected a predicate that returned a boolean, but the returning type was ${expressionReturnType.qualifiedName.parameterizedName}"
            )
         )
            .left()
      }
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)

      val filtered = collection.filter { collectionMember ->
         val factBag = FactBagValueSupplier.of(listOf(collectionMember), schema, thisScopeValueSupplier = objectFactory)
         val reader = AccessorReader(factBag, schema.functionRegistry, schema)
         val evaluated = reader.evaluate(
            collectionMember,
            expressionReturnType,
            deferredInstance.expression,
            dataSource = dataSource
         )

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

