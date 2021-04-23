package io.vyne.utils


/**
 * Eauality provides a convenient declarative way of building an alternative
 * equals and hashCode implementation for data classes.
 *
 * Equality caches calls to hashCode() so must not be used with classes that are not
 * immutable.
 */
class ImmutableEquality<T : Any>(val target: T, vararg val properties: T.() -> Any?) {
   companion object {
      const val DEFAULT_INITIAL_ODD_NUMBER = 17
      const val DEFAULT_MULTIPLIER_PRIME = 37
   }


   fun isEqualTo(other: Any?): Boolean {
      if (other == null) return false
      if (other === this) return true
      if (other.javaClass != target.javaClass) return false
      return properties.all { it.invoke(target) == it.invoke(other as T) }
   }

   private val hash = lazy {
      val fields = properties
         .map {
            val valueToHash = it.invoke(target)
            valueToHash?.hashCode() ?: 0
         }
      fields.fold(DEFAULT_INITIAL_ODD_NUMBER) { a, b -> DEFAULT_MULTIPLIER_PRIME * a + b }
   }

   fun hash(): Int {
      return hash.value
   }
}
