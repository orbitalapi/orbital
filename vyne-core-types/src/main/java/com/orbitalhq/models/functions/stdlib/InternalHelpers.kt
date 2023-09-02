package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.utils.log

inline fun <reified T> TypedInstance.valueAs(): T {
   return this.value as? T ?: error("Cannot cast ${this.value} to type ${T::class.simpleName}")
}

inline fun <reified T> TypedInstance.valueAs(successHandler: (T) -> TypedInstance, errorHandler: (Any?) -> TypedInstance):TypedInstance {
   val parsed = this.value as? T
   return if (parsed != null) {
      successHandler(parsed)
   } else {
      log().warn("Cannot cast ${this.value} to type ${T::class.simpleName}")
      errorHandler(this.value)
   }
}
