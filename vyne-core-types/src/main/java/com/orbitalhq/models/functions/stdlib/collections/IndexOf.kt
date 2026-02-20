package com.orbitalhq.models.functions.stdlib.collections

import arrow.core.getOrElse
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.argumentAsTypedInstanceOrError
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object IndexOf : NullSafeInvoker() {
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
      val collection = inputValues.argumentAsTypedInstanceOrError<TypedCollection>(0, returnType, function)
         .getOrElse { return it }
      val searchTarget = inputValues.argumentAsTypedInstanceOrError<TypedInstance>(1, returnType, function)
         .getOrElse { return it }

      val index = collection.indexOf(searchTarget)
      val source = EvaluatedExpression(function.asTaxi(), inputValues)
      return TypedInstance.from(returnType, index, schema, source = source)
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.IndexOfItem.name
}
