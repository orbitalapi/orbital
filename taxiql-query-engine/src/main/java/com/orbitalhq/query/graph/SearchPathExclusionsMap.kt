package com.orbitalhq.query.graph

class SearchPathExclusionsMap<K, V>(private val maxEntries: Int) : LinkedHashMap<K, V>() {
   override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
      return this.size > maxEntries
   }
}
