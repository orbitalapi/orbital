package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DeferredExpression
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.FactBagValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.facts.ScopedFact
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.TypeMatchingStrategy
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import lang.taxi.expressions.TypeExpression
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object Functional {
   val functions: List<NamedFunctionInvoker> = listOf(
      Reduce,
      Fold,
      MapFunction,
   )
}

object Fold : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Fold.name

   override suspend fun invoke(
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
      val deferredInstance = inputValues[2] as DeferredExpression
      val expression = deferredInstance.expression
      val expressionReturnType = schema.type(expression.returnType)
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      var accumulator: TypedValue = initialValue
      for (typedInstance in sourceCollection) {
         val factBagValueSupplier = FactBagValueSupplier.of(
            listOf(accumulator, typedInstance),
            schema,
            objectFactory,
            TypeMatchingStrategy.EXACT_MATCH
         )
         val reader = AccessorReader(factBagValueSupplier, schema.functionRegistry, schema)
         val evaluated =
            reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource, format = null)
         accumulator = evaluated as TypedValue
      }
      return accumulator
   }
}

object MapFunction : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Map.name

   private val logger = KotlinLogging.logger {}

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
      val sourceCollection = inputValues[0] as TypedCollection
      val deferredInstance = inputValues[1] as DeferredExpression
      val lambdaExpression = deferredInstance.expression
      val expressionReturnType = schema.type(lambdaExpression.returnType)
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      if (thisScopeValueSupplier !is TypedObjectFactory) {
         error("Cannot evaluate expression ${function.asTaxi()} as the Mapper has been constructed without a TypedObjectFactory (instead, it has a ${thisScopeValueSupplier::class.simpleName}, preventing resolving values)")
      }
      if (lambdaExpression.inputs.size != 1) {
         error("Cannot evaluate expression ${function.asTaxi()} as the function declares multiple inputs. This should've been detected by the compiler")
      }
      val mapFunctionInput = lambdaExpression.inputs[0]
      val result = mutableListOf<TypedInstance>()
      for (typedInstance in sourceCollection) {
         val scopedFact = if (typedInstance.type.taxiType.isAssignableTo(mapFunctionInput.type)) {
            ScopedFact(mapFunctionInput, typedInstance)
         } else {
            logger.error { "Map function expected iterable values of ${mapFunctionInput.type.toVyneQualifiedName().shortDisplayName}, but found an incompatible value of ${typedInstance.type.qualifiedName.shortDisplayName}. This should've been detected by the compiler. Mapping may not behave as expected" }
            ScopedFact(mapFunctionInput, typedInstance)
         }

         val evaluated = if (lambdaExpression.expression is TypeExpression) {
            thisScopeValueSupplier.newFactory(
               schema.type(lambdaExpression.expression.returnType), typedInstance,
               factsToExclude = setOf(sourceCollection),
               emptyList()
            )
               .build()
         } else {
            val factBag = FactBag.of(emptyList(), schema).withAdditionalScopedFacts(listOf(scopedFact), schema)
            thisScopeValueSupplier.newFactoryWithOnly(expressionReturnType, factBag)
               .evaluateExpression(lambdaExpression)
         }
         result.add(evaluated)
      }
      return if (result.isEmpty()) {
         TypedCollection.empty(returnType)
      } else {
         TypedCollection.from(result, dataSource)
      }
   }

}

object Reduce : NamedFunctionInvoker {
   override suspend fun invoke(
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
      val deferredInstance = inputValues[1] as DeferredExpression
      val expression = deferredInstance.expression
      val expressionReturnType = schema.type(expression.returnType)
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      var acc: TypedInstance = sourceCollection.first()
      for (i in 1 until sourceCollection.size) {
         val typedInstance = sourceCollection.toList()[i]
         val reader = AccessorReader.forFacts(listOf(acc, typedInstance), schema)
         acc = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = dataSource, format = null)
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
