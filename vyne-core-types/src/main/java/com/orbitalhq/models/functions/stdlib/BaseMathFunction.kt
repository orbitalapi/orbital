package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.AccessorReader
import com.orbitalhq.models.DeferredExpression
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.FactBagValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.TypeMatchingStrategy
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import java.math.BigDecimal


abstract class MathIteratingFunction : NullSafeInvoker() {
   override fun doInvoke(
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
      val expression = deferredInstance.expression
      val expressionReturnType = schema.type(expression.returnType)
      val inputValues = mutableListOf<TypedInstance>()

      val result = sourceCollection.fold(null as BigDecimal?) { acc, typedInstance ->
         val factBagValueSupplier = FactBagValueSupplier.of(
            listOf(typedInstance),
            schema,
            thisScopeValueSupplier,
            // Exact match so that the accumulated value (which is likely an INT) doesn't conflict with semantic subtypes.
            // We should be smarter about this.
            TypeMatchingStrategy.EXACT_MATCH
         )
         val reader = AccessorReader(factBagValueSupplier,schema.functionRegistry,schema)
         // Not sure what to pass as the data source here.  I hope that the actual expression evaluation will set the
         // data source correctly.
         val evaluated = reader.evaluate(typedInstance, expressionReturnType, expression, dataSource = typedInstance.source, format = null)
         inputValues.add(evaluated)
         val value = evaluated.value ?: return@fold acc ?: BigDecimal.ZERO
         val bigDecimalValue = when (value) {
            is Int -> value.toBigDecimal()
            is Double -> value.toBigDecimal()
            is Float -> value.toBigDecimal()
            is BigDecimal -> value
            else -> error("Don't know how to sum with type ${value::class}")
         }
         fold(acc, bigDecimalValue)
      } ?: BigDecimal.ZERO
      val castedResult = when (returnType.taxiType.basePrimitive!!) {
         PrimitiveType.INTEGER -> result.toInt()
         PrimitiveType.DOUBLE -> result.toDouble()
         PrimitiveType.DECIMAL -> result
         else -> result
      }
      val dataSource = EvaluatedExpression(
         function.asTaxi(),
         inputValues
      )
      return TypedValue.from(returnType, castedResult, source = dataSource)
   }

   abstract fun fold(acc: BigDecimal?, value: BigDecimal): BigDecimal
}
