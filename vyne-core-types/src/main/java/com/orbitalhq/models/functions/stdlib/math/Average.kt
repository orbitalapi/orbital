package com.orbitalhq.models.functions.stdlib.math

import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName
import java.math.BigDecimal
import java.math.RoundingMode

object Average : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Average.name

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
      val collectionToAverage = inputValues[0]
      if (collectionToAverage !is TypedCollection) {
         return createFailureWithTypedNull(
             "Expected a collection in arg 0, but got ${collectionToAverage.type.qualifiedName.shortDisplayName}",
             returnType,
             function,
             inputValues
         )
      }
      if (collectionToAverage.isEmpty()) {
         return createFailureWithTypedNull("Cannot average an empty collection", returnType, function, inputValues)
      }

      val invalidTypes = collectionToAverage.filter { !PrimitiveType.isNumberType(it.type.taxiType) }
      if (invalidTypes.isNotEmpty()) {
         return createFailureWithTypedNull(
             "Cannot average this collection, as it contains the following non-numeric types: ${
                 invalidTypes.map { it.type }.distinct().joinToString { it.qualifiedName.shortDisplayName }
             }", returnType, function, inputValues
         )
      }

      val totalTypes = collectionToAverage.map { it.type.taxiType.basePrimitive }
         .distinct()
      if (totalTypes.size != 1) {
         return createFailureWithTypedNull(
             "Cannot average a collection where the primitive types are mixed -- found ${totalTypes.joinToString { it?.qualifiedName ?: "null" }}",
             returnType,
             function,
             inputValues
         )
      }

      val valuesToAverage = collectionToAverage.map { it.value }
         .filterNotNull() as List<Number>
      val average = try {
         when (val firstNumber = valuesToAverage.first()) {
            is Int -> (valuesToAverage as List<Int>).average()
            is Long -> (valuesToAverage as List<Long>).average()
            is BigDecimal -> {
               val bigDecimals = (valuesToAverage as List<BigDecimal>)
               val sum = bigDecimals.reduce(BigDecimal::add)
               val average = sum.divide(bigDecimals.size.toBigDecimal(), RoundingMode.HALF_UP)
               average
            }
            is Double -> (valuesToAverage as List<Double>).average()
            is Float -> (valuesToAverage as List<Float>).average()
            else -> return createFailureWithTypedNull(
                "Unhandled JVM numeric type: ${firstNumber::class.simpleName}",
                returnType,
                function,
                inputValues
            )
         }
      } catch (e: Exception) {
         return createFailureWithTypedNull(
             "Failed to average collection: ${e.message}",
             returnType,
             function,
             inputValues
         )
      }
      return TypedInstance.from(
          returnType,
          average,
          schema,
          source = EvaluatedExpression(function.asTaxi(), inputValues)
      )
   }


}
