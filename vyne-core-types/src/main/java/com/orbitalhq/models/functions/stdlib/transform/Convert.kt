package com.orbitalhq.models.functions.stdlib.transform

import com.orbitalhq.models.*
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.SingleBy
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object Convert : NullSafeInvoker() {
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
      val source = inputValues[0]
      val targetType = inputValues[1] as TypeReferenceInstance
      val resultCacheKey = FunctionResultCacheKey(
         SingleBy.functionName,
         listOf(source, targetType)
      )
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val converted = resultCache.getOrPut(resultCacheKey) {
         TypedObjectFactory(targetType.type, FactBag.of(source, schema), schema, source = dataSource, functionRegistry = schema.functionRegistry)
            .build()
      }
      return converted as TypedInstance
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Convert.name

   private val logger = KotlinLogging.logger {}

}
