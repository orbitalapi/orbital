package com.orbitalhq.utils

import arrow.core.continuations.result

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
