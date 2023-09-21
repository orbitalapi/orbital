package com.orbitalhq.utils

fun Map<String,String>.obfuscateKeys(vararg keys:String):Map<String,String> {
   val result = this.toMutableMap()
   keys.forEach { key ->
      if (result.containsKey(key)) {
         result[key] = "********"
      }
   }
   return result
}
