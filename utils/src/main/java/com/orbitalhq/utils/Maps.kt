package com.orbitalhq.utils

import arrow.core.continuations.result
import org.eclipse.collections.impl.map.mutable.UnifiedMap

const val OBFUSCATED_VALUE = "*******"
fun Map<String,String>.obfuscateKeys(vararg keys:String):Map<String,String> {
   return this.obfuscateKeys(keys.toList()) { _,_-> OBFUSCATED_VALUE}
}

fun Map<String,String>.obfuscateKeys(keys:List<String>, obfuscator: (String,String) -> String):Map<String,String> {
   val result = this.toMutableMap()
   keys.forEach { key ->
      if (result.containsKey(key)) {
         result[key] = obfuscator(key, result[key]!!)
      }
   }
   return result
}

/**
 * Merges two maps, returning the result as a UnifiedMap from EclipseCollections.
 * UnifiedMap is a high performance map
 */
fun <K,V> Map<K,V>?.mergeToUnifiedMap(other:Map<K,V>):Map<K,V> {
   val map = if (this == null) {
      UnifiedMap()
   } else {
      UnifiedMap(this)
   }
   map.putAll(other)
   return map
}
