package io.vyne.models.functions

import io.vyne.models.TypedInstance
import io.vyne.models.functions.stdlib.StdLib
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.Function
import lang.taxi.functions.FunctionAccessor

class FunctionRegistry(private val invokers: List<NamedFunctionInvoker>) {
   private val invokersByName = invokers.associateBy { it.functionName }
   val taxiDeclaration = invokers
      .filterIsInstance<SelfDescribingFunction>()
      .joinToString("\n") { it.taxiDeclaration }

   fun invoke(
      function: Function,
      declaredInputs: List<TypedInstance>,
      schema: Schema,
      returnType: Type,
      accessor: FunctionAccessor
   ): TypedInstance {
      val invoker = invokersByName[function.toQualifiedName()]
         ?: error("No invoker provided for function ${function.qualifiedName}")
      return invoker.invoke(declaredInputs, schema, returnType, accessor)
   }

   companion object {
      val default: FunctionRegistry = FunctionRegistry(StdLib.functions)
   }

   fun add(invoker: NamedFunctionInvoker) : FunctionRegistry {
      return add(listOf(invoker))
   }
   fun add(invokers: List<NamedFunctionInvoker>): FunctionRegistry {
      return FunctionRegistry(this.invokers + invokers)
   }
}

