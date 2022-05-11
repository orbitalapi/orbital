package io.vyne.utils

inline fun <reified T : Any> Any.asA(): T {
   return this as T
}
