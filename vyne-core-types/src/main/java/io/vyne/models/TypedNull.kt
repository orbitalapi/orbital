package io.vyne.models

import com.google.common.cache.CacheBuilder
import io.vyne.schemas.Type
import lang.taxi.Equality

// TypedNull is very cachable, except for the source attribute.
// So, we create an internal wrapper, and cache that.
// Created during perf optimisation that found c. 5% of operations were on hashCode / creation
// of typed null
private data class TypedNullWrapper(val type: Type) {
   private val equality = Equality(this, TypedNullWrapper::type)
   private val hash by lazy { this.equality.hash() }
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = hash
}

class TypedError(override val type: Type, val errorMessage: String, override val source: DataSource = UndefinedSource) :
   TypedInstance {
   override val value: Any? = null
   override fun withTypeAlias(typeAlias: Type): TypedInstance = TypedError(typeAlias, errorMessage, source)

   override fun valueEquals(valueToCompare: TypedInstance) = false
}

class TypedNull private constructor(
   private val wrapper: TypedNullWrapper,
   override val source: DataSource = UndefinedSource
) : TypedInstance {
   companion object {
      private val cache = CacheBuilder.newBuilder()
         .build<Type, TypedNullWrapper>()

      fun create(type: Type, source: DataSource = UndefinedSource): TypedNull {
         val wrapper = cache.get(type) {
            TypedNullWrapper(type)
         }
         return TypedNull(wrapper, source)
      }
   }

   override val type: Type = wrapper.type
   private val equality = Equality(this, TypedNull::type, TypedNull::wrapper)
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
   override val value: Any? = null
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedNull.create(typeAlias, this.source)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      return valueToCompare.value == null
   }
}
