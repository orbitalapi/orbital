package com.orbitalhq.functions.taxigen

import com.orbitalhq.functions.scanner.ArgBinding
import com.orbitalhq.functions.scanner.BoundFunction
import lang.taxi.jvm.common.PrimitiveTypes

/**
 * Generates Taxi function definitions from BoundFunctions.
 *
 * Supports:
 *  - Namespace declaration (taken from the provider class package)
 *  - Function name + description
 *  - @TaxiParam for parameter names and explicit Taxi types
 *  - Primitive type mapping via PrimitiveTypes
 *  - Kotlin nullability => optional parameter
 */
object TaxiFunctionDefinitionGenerator {

   fun generate(functions: List<BoundFunction>, overrideNamespace: String? = null): String {
      val builder = StringBuilder()

      functions.groupBy { it.namespace }
         .forEach { (namespace, functions) ->
            // Derive namespace: either caller-provided, or package of first function’s declaring class
            val ns = overrideNamespace ?: namespace
            builder.appendLine("namespace $ns {")
            builder.appendLine()

            functions.forEach { fn ->
               if (fn.description.isNotBlank()) {
                  builder.appendLine("[[")
                  builder.appendLine(fn.description.trim())
                  builder.appendLine("]]")
               }

               val params = fn.bindings.mapIndexed { idx, binding ->
                  when (binding) {
                     is ArgBinding -> {
                        val paramName =  binding.paramName
                        val taxiType = binding.taxiType().let {
                           if (binding.nullable) "$it?" else it
                        }
                        "$paramName: $taxiType"
                     }

                     else -> null // schema/returnType/etc not exposed in Taxi signature
                  }
               }.filterNotNull()

               val returnType = fn.returnTaxiType()

               builder.appendLine(
                  "declare function ${fn.name}(${params.joinToString(", ")}): $returnType"
               )
               builder.appendLine()
            }


            builder.appendLine("}") // close the namespace block
         }


      return builder.toString().trim()
   }

   private fun ArgBinding.taxiType(): String {
      return when {
         !explicitTaxiType.isNullOrEmpty() -> explicitTaxiType
         expectedType.isPrimitiveOrBoxedOrKnown() -> PrimitiveTypes.getTaxiPrimitive(expectedType).qualifiedName
         else ->
            // Fallback: use @TaxiParam.type or class name
            expectedType.simpleName
      }
   }

   private fun BoundFunction.returnTaxiType(): String {
      if (this.suppliedReturnType != null) {
         return this.suppliedReturnType
      }
      val innerType: Class<*> = this.jvmReturnType.unwrapContainerType()

      return if (PrimitiveTypes.isClassTaxiPrimitive(innerType)) {
         PrimitiveTypes.getTaxiPrimitive(innerType).qualifiedName
      } else {
         innerType.simpleName
      }
   }

   private fun Class<*>.isPrimitiveOrBoxedOrKnown(): Boolean {
      return PrimitiveTypes.isClassTaxiPrimitive(this)
   }
}
