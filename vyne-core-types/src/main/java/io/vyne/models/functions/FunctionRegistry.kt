package io.vyne.models.functions

import io.vyne.models.TypedInstance
import io.vyne.models.functions.stdlib.StdLib
import io.vyne.schemas.Schema
import lang.taxi.functions.Function
import lang.taxi.types.QualifiedName

class FunctionRegistry(private val invokers: List<FunctionInvoker>) {
   private val invokersByName = invokers.associateBy { it.functionName }
   val taxiDeclaration = invokers
      .filterIsInstance<SelfDescribingFunction>()
      .joinToString("\n") { it.taxiDeclaration }

   fun invoke(function: Function, declaredInputs: List<TypedInstance>, schema: Schema): TypedInstance {
      val invoker = invokersByName[function.toQualifiedName()]
         ?: error("No invoker provided for function ${function.qualifiedName}")
      return invoker.invoke(declaredInputs, schema)
   }

   companion object {
      fun default(): FunctionRegistry {
         return FunctionRegistry(StdLib.functions)
      }
   }
}

interface FunctionInvoker {
   val functionName: QualifiedName

   fun invoke(inputValues: List<TypedInstance>, schema: Schema): TypedInstance
}

interface SelfDescribingFunction : FunctionInvoker {
   val taxiDeclaration: String
}
