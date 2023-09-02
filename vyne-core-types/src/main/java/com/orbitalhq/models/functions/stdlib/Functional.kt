package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DeferredTypedInstance
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.FactBagValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.TypeMatchingStrategy
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object Functional {
   val functions: List<NamedFunctionInvoker> = listOf(
      Reduce,
      Fold,
      Sum,
      Max,
      Min

   )
}

object Fold : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Fold.name

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
      val sourceCollection = inputValues[0] as TypedCollection
      val initialValue = inputValues[1] as TypedValue
      val deferredInstance = inputValues[2] as DeferredTypedInstance
      val expression = deferredInstance.expression
      val expressionReturnType = schema.type(expression.returnType)
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      val foldedValue = sourceCollection.fold(initialValue) { acc,typedInstance ->
         val factBagValueSupplier = FactBagValueSupplier.of(
            listOf(acc,typedInstance),
            schema,
            objectFactory,
            // Exact match so that the accumulated value (which is likely an INT) doesn't conflict with semantic subtypes.
            // We should be smarter about this.
            TypeMatchingStrategy.EXACT_MATCH
         )
         val reader = AccessorReader(factBagValueSupplier,schema.functionRegistry,schema)
         val evaluated = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource, format = null)
         evaluated as TypedValue
      }
      return foldedValue
   }
}

object Reduce : NamedFunctionInvoker {
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
      val sourceCollection = inputValues[0] as TypedCollection
      val deferredInstance = inputValues[1] as DeferredTypedInstance
      val expression = deferredInstance.expression
      val expressionReturnType = schema.type(expression.returnType)
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      sourceCollection.reduce { acc, typedInstance ->
         val reader = AccessorReader.forFacts(listOf(acc, typedInstance), schema)
         val evaluated = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource, format = null)
         evaluated
      }
      sourceCollection.forEach { instance ->
//         val reader = AccessorReader(SimpleValueStore(listOf(instance), schema), schema.functionRegistry, schema)
//         val result = reader.evaluate(instance, schema.type(expression.returnType), expression, schema, emptySet(), UndefinedSource)
         TODO()
      }
      TODO("Not yet implemented")
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Reduce.name
}
