package com.orbitalhq.functions.scanner

import io.vavr.control.Either
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed class JvmReturnType {
   data class Kotlin(val kType: KType) : JvmReturnType()
   data class Java(val jType: java.lang.reflect.Type) : JvmReturnType()

   fun erasedClass(): Class<*> = when (this) {
      is Kotlin -> (kType.classifier as? KClass<*>)?.java
         ?: throw IllegalStateException("Unsupported KType classifier: ${kType.classifier}")
      is Java -> when (jType) {
         is Class<*> -> jType
         is ParameterizedType -> jType.rawType as Class<*>
         is GenericArrayType -> {
            val component = jType.genericComponentType
            java.lang.reflect.Array.newInstance(
               if (component is Class<*>) component else Any::class.java,
               0
            ).javaClass
         }
         else -> throw IllegalStateException("Unsupported Java Type: $jType")
      }
   }

   fun displayName(): String = when (this) {
      is Kotlin -> kType.toString()
      is Java -> jType.typeName
   }

   /**
    * If this type is a container (Kotlin Result<T>, Vavr Either<L,R>),
    * unwrap to the contained "success" type.
    */
   fun unwrapContainerType(): Class<*> {
      return when (this) {
         is Kotlin -> {
            val classifier = kType.classifier as? KClass<*> ?: return erasedClass()
            when (classifier) {
               Result::class -> {
                  val arg = kType.arguments.firstOrNull()?.type
                  (arg?.classifier as? KClass<*>)?.java ?: Any::class.java
               }

               Either::class -> {
                  val arg = kType.arguments.getOrNull(1)?.type
                  (arg?.classifier as? KClass<*>)?.java ?: Any::class.java
               }

               else -> erasedClass()
            }
         }

         is Java -> when (jType) {
            is ParameterizedType -> when (jType.rawType) {
               Result::class.java -> {
                  val arg = jType.actualTypeArguments.firstOrNull()
                  toClass(arg) ?: Any::class.java
               }

               Either::class.java -> {
                  val arg = jType.actualTypeArguments.getOrNull(1)
                  toClass(arg) ?: Any::class.java
               }

               else -> erasedClass()
            }

            else -> erasedClass()
         }
      }
   }

   private fun toClass(type: java.lang.reflect.Type?): Class<*>? = when (type) {
      is Class<*> -> type
      is ParameterizedType -> type.rawType as? Class<*>
      else -> null
   }
}
