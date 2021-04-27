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

class KeyCached<K, V>(private val factory: (K) -> V) {
   private val store = mutableMapOf<K, V>()

   fun invalidate() {
      store.clear()
   }

   fun get(key: K): V {
      return store.getOrPut(key) { factory(key) }
   }

   fun removeValues(predicate: (K, V) -> Boolean) {
      synchronized(this.store) {
         val keysToRemove = this.store.mapNotNull { (key, value) ->
            val shouldRemove = predicate(key, value)
            if (shouldRemove) {
               key
            } else {
               null
            }
         }
         keysToRemove.forEach { this.store.remove(it) }
      }
   }
}

fun <K, V> cached(factory: (K) -> V): KeyCached<K, V> = KeyCached(factory)
