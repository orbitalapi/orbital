package com.orbitalhq.utils

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import org.eclipse.collections.impl.map.mutable.UnifiedMap

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
   private val store = ConcurrentHashMap<K,V>()

   fun invalidate() {
      store.clear()
   }

   fun get(key: K): V {
      return store.getOrPut(key) { factory(key) }
   }
}

fun <K, V> cached(factory: (K) -> V): KeyCached<K, V> = KeyCached(factory)
