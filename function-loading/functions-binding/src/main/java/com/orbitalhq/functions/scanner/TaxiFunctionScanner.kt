package com.orbitalhq.functions.scanner

import com.orbitalhq.functions.TaxiFunction
import com.orbitalhq.functions.TaxiFunctionProvider
import com.orbitalhq.functions.scanner.ParameterBindings.buildBindingsJava
import com.orbitalhq.functions.scanner.ParameterBindings.buildBindingsKotlin
import mu.KotlinLogging
import java.lang.invoke.MethodHandles
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod


/**
 * Scans a provider instance for methods annotated with @TaxiFunction.
 * Supports both Kotlin reflection (preferred for Kotlin classes)
 * and Java reflection (for pure Java providers).
 */
class TaxiFunctionScanner {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   fun scan(providers: List<TaxiFunctionProvider>):List<BoundFunction> {
      return providers.flatMap { instance ->
         scan(instance)
      }
   }
   // TODO : This should be TaxiFunctionProvider, not Any
   fun scan(provider: TaxiFunctionProvider): List<BoundFunction> {
      val kClass = provider::class
      val results = mutableListOf<BoundFunction>()


      // Prefer Kotlin reflection: richer metadata, nullability info
      kClass.members
         .filterIsInstance<kotlin.reflect.KFunction<*>>()
         .forEach { kFunction ->
            val fnAnn = kFunction.findAnnotation<TaxiFunction>() ?: return@forEach
            val jMethod = kFunction.javaMethod ?: return@forEach
            val bindings = buildBindingsKotlin(kFunction, jMethod)
            val mh = MethodHandles.lookup().unreflect(jMethod).bindTo(provider)

            val namespace = jMethod.declaringClass.packageName
            results.add(
               BoundFunction(
                  name = if (fnAnn.name.isNotBlank()) fnAnn.name else jMethod.name,
                  namespace = namespace,
                  description = fnAnn.description,
                  bindings = bindings,
                  suppliedReturnType = fnAnn.returnType.takeUnless { it.isNullOrEmpty()},
                  methodHandle = mh,
                  jvmReturnType = JvmReturnType.Kotlin(kFunction.returnType)
               )
            )
         }

      // Java reflection: catch any methods not surfaced through Kotlin reflection
      provider::class.java.methods
         .filter { it.isAnnotationPresent(TaxiFunction::class.java) }
         .forEach { jMethod ->
            if (results.any { it.name == jMethod.name }) return@forEach // skip dup
            val fnAnn = jMethod.getAnnotation(TaxiFunction::class.java)
            val bindings = buildBindingsJava(jMethod)
            val mh = MethodHandles.lookup().unreflect(jMethod).bindTo(provider)

            results.add(
               BoundFunction(
                  name = if (fnAnn.name.isNotBlank()) fnAnn.name else jMethod.name,
                  namespace = jMethod.declaringClass.packageName,
                  description = fnAnn.description,
                  bindings = bindings,
                  suppliedReturnType = fnAnn.returnType.takeUnless { it.isNullOrEmpty()},
                  methodHandle = mh,
                  jvmReturnType = JvmReturnType.Java(jMethod.genericReturnType)
               )
            )
         }
      logger.info { "Custom function provider ${provider::class.simpleName} provided the following functions: ${results.joinToString { it.functionName.parameterizedName }}" }

      return results
   }
}
