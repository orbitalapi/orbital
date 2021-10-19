package io.vyne.models.functions.stdlib

import io.vyne.models.AccessorReader
import io.vyne.models.DeferredTypedInstance
import io.vyne.models.EvaluatedExpression
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.FactBagValueSupplier
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.TypeMatchingStrategy
import lang.taxi.functions.FunctionAccessor
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
      objectFactory: EvaluationValueSupplier
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
            // Exact match so that the accumulated value (which is likely an INT) doesn't conflict with semantic subtypes.
            // We should be smarter about this.
            TypeMatchingStrategy.EXACT_MATCH
         )
         val reader = AccessorReader(factBagValueSupplier,schema.functionRegistry,schema)
         val evaluated = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource)
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
      objectFactory: EvaluationValueSupplier
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
         val evaluated = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource)
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
