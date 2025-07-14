package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.*
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.NullSafeInvoker
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.PrimitiveType
import lang.taxi.types.QualifiedName

object ObjectFunctions {
   val functions:List<NamedFunctionInvoker> = listOf(
      Equals,
      EmptyInstance
   )
}

object Equals : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.Equals.name

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
      val areEqual = inputValues[0] == inputValues[1]
      return TypedValue.from(schema.type(PrimitiveType.BOOLEAN), areEqual, ConversionService.DEFAULT_CONVERTER, EvaluatedExpression(function.asTaxi(), inputValues))
   }
}


object EmptyInstance : NullSafeInvoker() {
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
      val typeReference = inputValues[0]
      if (typeReference !is TypeReferenceInstance) {
         return createFailureWithTypedNull("Expected arg 0 to be a TypeReference, but instead got ${typeReference::class.simpleName}", returnType, function, inputValues)
      }
      val type = typeReference.type
      val empty = buildEmptyMap(type, schema)
      val dataSource = EvaluatedExpression(function.asTaxi(), inputValues)
      val result = TypedInstance.from(type, empty, schema, source = dataSource)
      return result
   }

   private fun buildEmptyMap(type: Type, schema: Schema): Any? {
      return when {
         type.isScalar -> null
         type.isCollection -> emptyList<Any?>()
         type.isStream -> emptyList<Any?>()
         else -> {
            type.attributes.map { (name,field) ->
               name to buildEmptyMap(schema.type(field.type), schema)
            }.toMap()
         }
      }
   }

   override val functionName: QualifiedName = lang.taxi.functions.stdlib.EmptyInstance.name
}
