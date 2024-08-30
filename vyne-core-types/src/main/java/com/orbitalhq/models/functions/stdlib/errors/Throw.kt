package com.orbitalhq.models.functions.stdlib.errors

import com.orbitalhq.errors.QueryErrors
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.functionFailed
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object Throw : NamedFunctionInvoker {
   override val functionName: QualifiedName =  lang.taxi.functions.stdlib.Throw.name

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
      val error = inputValues.getOrNull(0)
         ?: functionFailed(returnType, function, inputValues, "Expected an error as input 0, but no inputs were present")


      // Normally, we'd return a TypedNull here.
      // But as this method's job is to throw an exception, that'd be the wrong choice.
      if (error is TypedNull) {
         failWithException("Attempted to call throw() without passing an exception to throw")
      }


      val responseHeaders = objectFactory.inPlaceQueryEngine?.populateResponseHeaders() ?: emptyMap()

      val exception = QueryErrors.toException(error, schema, responseHeaders)
      throw exception
   }

   private fun failWithException(message: String):Nothing {
      throw IllegalStateException(message)
   }

}
