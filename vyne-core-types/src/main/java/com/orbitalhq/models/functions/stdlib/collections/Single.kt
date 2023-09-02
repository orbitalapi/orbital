package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.getOrHandle
import com.google.common.base.Stopwatch
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName
import mu.KotlinLogging

object Single : NamedFunctionInvoker, CollectionFilteringFunction() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Single.name
private val logger = KotlinLogging.logger {}
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
      val stopwatch = Stopwatch.createStarted()

      // TODO : does it always make sense to cache this?  Is there a heuristic we can use to consider
      // when it's not appropriate?


      val result = applyFilter(inputValues, schema, returnType, function, objectFactory, rawMessageBeingParsed)
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
      logger.debug { "Single completed in ${stopwatch.elapsed().toMillis()}ms" }
      return result
   }
}
