package com.orbitalhq.models

import com.google.common.collect.Interners
import com.orbitalhq.schemas.Type
import com.orbitalhq.utils.Ids
import com.orbitalhq.utils.ImmutableEquality
import lang.taxi.utils.log
import lang.taxi.utils.takeHead

// TypedNull is very cachable, except for the source attribute.
// So, we create an internal wrapper, and cache that.
// Created during perf optimisation that found c. 5% of operations were on hashCode / creation
// of typed null
private data class TypedNullWrapper(val type: Type) {
   private val cachedHashCode: Int = type.hashCode()

   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is TypedNullWrapper) return false

      return type == other.type
   }

   override fun hashCode(): Int {
      return cachedHashCode
   }
}

data class TypedNull private constructor(private val wrapper: TypedNullWrapper,
                                    override val source: DataSource = UndefinedSource) : TypedInstance {

   init {
       if (source is UndefinedSource) {
          log().debug("Found a TypedNull with an UndefinedSource.  Consider updating caller to populate the datasource")
       }
   }
   override val nodeId: String = Ids.fastUuid()
   override val metadata: Map<String, Any> = emptyMap()

   companion object {
      // Intern the wrappers, so that we can do fast equality checks
      private val internedWrappers = Interners.newWeakInterner<TypedNullWrapper>()

      fun create(type: Type, source: DataSource = UndefinedSource): TypedNull {
         val wrapper = internedWrappers.intern(TypedNullWrapper(type))
         return TypedNull(wrapper, source)
      }

      fun isFailedSearch(typedInstance: TypedInstance):Boolean {
         return typedInstance is TypedNull && typedInstance.source is FailedSearch
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

   /**
    * Returns a typed null for every property in the provided path (foo.bar.baz).
    *
    * This is needed when populating a property path, and encountering a null.
    * eg: In foo.bar.baz, if bar is null, then so is baz.
    */
   fun nullsForPropertyPath(path: String): List<TypedNull> {
      val parts = path.split(".")
      val (thisPropertyName, remaining) = parts.takeHead()
      val attributeTypeName = this.type.attribute(thisPropertyName).type
      val attributeType = this.type.typeCache.type(attributeTypeName)
      val thisTypedNull = create(attributeType, this.source)

      return if (remaining.isEmpty()) {
         listOf(thisTypedNull)
      } else {
         listOf(thisTypedNull) + thisTypedNull.nullsForPropertyPath(remaining.joinToString("."))
      }
   }
}
