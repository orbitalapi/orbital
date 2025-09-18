package com.orbitalhq.functions.scanner

import com.orbitalhq.functions.ReturnType
import com.orbitalhq.functions.TaxiParam
import com.orbitalhq.models.EvaluationValueSupplier
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.functions.FunctionAccessor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

sealed interface ParameterBinding {
   fun resolve(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessage: Any?,
   ): Any?
}

data class ArgBinding(
   val index: Int,
   val expectedType: Class<*>,
   val paramName: String,
   val explicitTaxiType: String? = null,
   val nullable: Boolean = false
) : ParameterBinding {
   override fun resolve(
      inputValues: List<TypedInstance>, schema: Schema, returnType: Type,
      function: FunctionAccessor, objectFactory: EvaluationValueSupplier,
      rawMessage: Any?
   ): Any? {
      val arg = inputValues[index]
      return if (expectedType == TypedInstance::class.java) arg else arg.value
   }
}

object SchemaBinding : ParameterBinding {
   override fun resolve(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessage: Any?
   ): Any = schema
}

object FunctionAccessorBinding : ParameterBinding {
   override fun resolve(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessage: Any?
   ): Any? = function
}

object ReturnTypeBinding : ParameterBinding {
   override fun resolve(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessage: Any?
   ): Any = returnType
}


object EvaluationValueSupplierBinding : ParameterBinding {
   override fun resolve(
      inputValues: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      function: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      rawMessage: Any?
   ): Any = objectFactory
}

object ParameterBindings {
   fun buildBindingsKotlin(kFunction: KFunction<*>, jMethod: Method): List<ParameterBinding> {
      return kFunction.parameters.filter { it.kind == KParameter.Kind.VALUE }.mapIndexed { idx, param ->
         val taxiParam = param.findAnnotation<TaxiParam>()
         when {
            taxiParam != null -> ArgBinding(
               index = idx,
               expectedType = jMethod.parameterTypes[idx],
               paramName = taxiParam.name.ifBlank { param.name ?: "arg$idx" },
               explicitTaxiType = taxiParam.type.takeIf { it.isNotBlank() },
               nullable = taxiParam.nullable || param.type.isMarkedNullable
            )
            param.findAnnotation<ReturnType>() != null -> ReturnTypeBinding
            param.type.classifier == EvaluationValueSupplier::class -> EvaluationValueSupplierBinding
            param.type.classifier == Schema::class -> SchemaBinding
            param.type.classifier == FunctionAccessor::class -> FunctionAccessorBinding
            else -> error("Unsupported parameter: $param")
         }
      }
   }

   fun buildBindingsJava(method: Method): List<ParameterBinding> {
      return method.parameters.mapIndexed { idx, param ->
         val taxiParam = param.getAnnotation(TaxiParam::class.java)
         when {
            taxiParam != null -> ArgBinding(
               index = idx,
               expectedType = param.type,
               paramName = taxiParam.name.ifBlank { param.name ?: "arg$idx" },
               explicitTaxiType = taxiParam.type.takeIf { it.isNotBlank() },
               nullable = taxiParam.nullable
            )
            param.isAnnotationPresent(ReturnType::class.java) -> ReturnTypeBinding
            param.type == Schema::class.java -> SchemaBinding
            param.type == EvaluationValueSupplier::class.java -> EvaluationValueSupplierBinding
            param.type == FunctionAccessor::class.java -> FunctionAccessorBinding
            else -> error("Unsupported parameter: $param")
         }
      }
   }
}
