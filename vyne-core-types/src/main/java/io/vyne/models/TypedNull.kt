package io.vyne.models

import io.vyne.schemas.Type
import io.vyne.utils.ImmutableEquality
import lang.taxi.Equality
import lang.taxi.packages.utils.log

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

data class TypedNull private constructor(private val wrapper: TypedNullWrapper,
                                    override val source: DataSource = UndefinedSource) : TypedInstance {

   companion object {
      // Disabling the Cache as it is holding up significant amount of heap memory that can't be reclaimed.
      //private val cache = CacheBuilder.newBuilder()
      //   .build<Type, TypedNullWrapper>()

      fun create(type: Type, source: DataSource = UndefinedSource): TypedNull {
         //val wrapper = cache.get(type) {
         //   TypedNullWrapper(type)
         //}
         return TypedNull(TypedNullWrapper(type), source)
      }
   }

   override val type: Type = wrapper.type
   private val equality = ImmutableEquality(this, TypedNull::type)
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = equality.hash()
   override val value: Any? = null
   override fun toString(): String {
      return "TypedNull(type=${wrapper.type.fullyQualifiedName})"
   }
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedNull.create(typeAlias, this.source)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      return valueToCompare.value == null
   }
}
