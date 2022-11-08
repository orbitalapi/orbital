package io.vyne.models.functions.stdlib.collections

import arrow.core.getOrHandle
import com.google.common.base.Stopwatch
import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.TypedInstance
import io.vyne.models.functions.FunctionResultCacheKey
import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.FunctionAccessor
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
