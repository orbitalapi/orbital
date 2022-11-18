package io.vyne.models

import com.google.common.collect.Interners
import io.vyne.schemas.Type
import io.vyne.utils.ImmutableEquality
import lang.taxi.Equality
import lang.taxi.packages.utils.log

// TypedNull is very cachable, except for the source attribute.
// So, we create an internal wrapper, and cache that.
// Created during perf optimisation that found c. 5% of operations were on hashCode / creation
// of typed null
private data class TypedNullWrapper(val type: Type) {
   private val equality = ImmutableEquality(this, TypedNullWrapper::type)
   private val hash by lazy { this.equality.hash() }
   override fun equals(other: Any?): Boolean = equality.isEqualTo(other)
   override fun hashCode(): Int = hash
}

data class TypedNull private constructor(private val wrapper: TypedNullWrapper,
                                    override val source: DataSource = UndefinedSource) : TypedInstance {

   init {
       if (source is UndefinedSource) {
          log().debug("Found a TypedNull with an UndefinedSource.  Consider updating caller to populate the datasource")
       }
   }
   companion object {
      // Intern the wrappers, so that we can do fast equality checks
      private val internedWrappers = Interners.newWeakInterner<TypedNullWrapper>()

      fun create(type: Type, source: DataSource = UndefinedSource): TypedNull {
         val wrapper = internedWrappers.intern(TypedNullWrapper(type))
         return TypedNull(wrapper, source)
      }
   }

   override val type: Type = wrapper.type
   private val equality = ImmutableEquality(this, TypedNull::type)
   override fun equals(other: Any?): Boolean {
      // Don't call equality.equals() here, as it's too slow.
      // We need a fast, non-reflection based implementation.
      if (this === other) return true
      if (other == null) return false
      if (this.javaClass !== other.javaClass) return false
      // wrapper has been interned, so check for object reference equality
      return this.wrapper === (other as TypedNull).wrapper
   }
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
