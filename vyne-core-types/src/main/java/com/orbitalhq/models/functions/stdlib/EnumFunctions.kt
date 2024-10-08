package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.EnumValueKind
import com.orbitalhq.models.EvaluatedExpression
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypeReferenceInstance
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedValue
import com.orbitalhq.models.functions.FunctionResultCacheKey
import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.stdlib.collections.createFailureWithTypedNull
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.QualifiedName

object EnumFunctions {
   val functions : List<NamedFunctionInvoker> = listOf(
      com.orbitalhq.models.functions.stdlib.EnumForName
   )
}


object EnumForName : NamedFunctionInvoker {
   override val functionName: QualifiedName = lang.taxi.functions.stdlib.EnumForName.name

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
      val typeReference = inputValues[0] as TypeReferenceInstance

      // Important: TypeReferenceInstances are cached, so the type that's returned
      val enumType = typeReference.resolveType(schema)
      val value = inputValues[1] as TypedValue

      if (!enumType.isEnum) {
         return createFailureWithTypedNull("Expected an enum type, but ${enumType.qualifiedName.shortDisplayName} is not an enum type",
            returnType, function, inputValues
            )
      }
      if (value.value !is String) {
         return createFailureWithTypedNull("Expected to be passed a valid enum name in param 1, but got ${value.value ?: "null"}",
            returnType, function, inputValues
         )
      }
      val source = EvaluatedExpression(function.asTaxi(), inputValues)
      val result = try {
         enumType.enumTypedInstance(value.value as String, source, EnumValueKind.VALUE)
      } catch (e:Exception) {
         createFailureWithTypedNull("Lookup of enum by name failed - ${e.message!!}", returnType, function, inputValues)
      }
      return result
   }

}
