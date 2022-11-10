package io.vyne.models.functions

import io.vyne.models.EvaluationValueSupplier
import io.vyne.models.TypedInstance
import io.vyne.models.functions.stdlib.StdLib
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import lang.taxi.functions.Function
import lang.taxi.functions.FunctionAccessor
import lang.taxi.types.FormatsAndZoneOffset

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
      accessor: FunctionAccessor,
      objectFactory: EvaluationValueSupplier,
      returnTypeFormat: FormatsAndZoneOffset?,
      /**
       * The raw value / message being parsed.
       * Not always present, but passed when evaluating from TypedObjectFactory
       */
      rawMessageBeingParsed: Any? = null,
      /**
       * A result cache allows functions to cache / memoize results
       * so that future computations can be faster.
       * Consider that everything should be serializable, so that we can distribute the cache over Hazelcast
       * DO NOT use this for sharing data between function calls. Seriously.
       *
       */
      resultCache: MutableMap<FunctionResultCacheKey, Any> = mutableMapOf()
   ): TypedInstance {
      val invoker = invokersByName[function.toQualifiedName()]
         ?: error("No invoker provided for function ${function.qualifiedName}")
      return invoker.invoke(
         declaredInputs,
         schema,
         returnType,
         accessor,
         objectFactory,
         returnTypeFormat         ,
         rawMessageBeingParsed,
         resultCache
      )
   }

   companion object {
      val default: FunctionRegistry = FunctionRegistry(StdLib.functions)
   }

   fun add(invoker: NamedFunctionInvoker): FunctionRegistry {
      return add(listOf(invoker))
   }

   fun add(invokers: List<NamedFunctionInvoker>): FunctionRegistry {
      return FunctionRegistry(this.invokers + invokers)
   }

   fun merge(functionRegistry: FunctionRegistry): FunctionRegistry {
      return FunctionRegistry(
         (this.invokers + functionRegistry.invokers).distinctBy { it.functionName }
      )
   }
}

