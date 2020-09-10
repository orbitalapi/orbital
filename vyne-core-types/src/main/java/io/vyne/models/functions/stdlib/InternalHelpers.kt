package io.vyne.models.functions.stdlib

import io.vyne.models.TypedInstance

inline fun <reified T> TypedInstance.valueAs(): T {
   return this.value as? T ?: error("Cannot cast ${this.value} to type ${T::class.simpleName}")
}
