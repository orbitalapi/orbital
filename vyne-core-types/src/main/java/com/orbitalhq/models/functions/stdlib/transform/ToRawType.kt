package com.orbitalhq.models.functions.stdlib.transform

import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedMaps
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object ToRawType : NullSafeInvoker() {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.ToRawType.name
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
      val input = inputValues.first()
      val source = EvaluatedExpression(function.asTaxi(), inputValues)
      return stripTypes(input, schema, source, returnType)
   }

   private fun stripTypes(
      input: TypedInstance,
      schema: Schema,
      source: EvaluatedExpression,
      returnType: Type
   ): TypedInstance {
      return when (input) {
         is TypedNull -> input
         is TypedValue -> return DataSourceUpdater.update(
            input.withTypeAlias(schema.type(input.type.basePrimitiveTypeName!!)),
            source
         )

         is TypedCollection -> {
            val strippedMembers = input.map { stripTypes(it, schema, source, returnType) }
            if (strippedMembers.isEmpty()) {
               TypedCollection.empty(returnType.asArrayType(), source)
            } else {
               TypedCollection.from(strippedMembers, source)
            }
         }

         is TypedObject -> {
            val stripped = input.value.mapValues { (name, value) -> stripTypes(value, schema, source, value.type) }
            TypedObject.fromAttributes(schema.type(PrimitiveType.ANY), stripped, schema, source = source)
         }
         else -> error("StripTypes not implemented for value of kind ${input::class.simpleName}")
      }

   }
}
