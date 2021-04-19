package io.vyne.utils

class Cached<T>(private val factory: () -> T) {
   companion object {
      private const val KEY = "KEY"
   }

   private val store = mutableMapOf<String, T>()

   fun invalidate() {
      store.clear()
   }

   fun get(): T {
      return store.getOrPut(KEY, factory)
   }
}

fun <T> cached(factory: () -> T): Cached<T> = Cached(factory)

