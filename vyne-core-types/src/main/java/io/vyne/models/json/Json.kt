package io.vyne.models.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun isJson(value: Any): Boolean {
   if (value !is String) return false
   val trimmed = value.trim()
   return when {
      trimmed.startsWith("{") && trimmed.endsWith("}") -> true
      isJsonArray(value) -> true
      else -> false
   }
}

fun isJsonArray(value: Any): Boolean {
   if (value !is String) return false
   val trimmed = value.trim()
   return when {
      trimmed.startsWith("[") && trimmed.endsWith("]") -> true
      else -> false
   }
}

object Jackson {
   val defaultObjectMapper: ObjectMapper by lazy {
      jacksonObjectMapper()
         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false)
         .registerModule(JavaTimeModule())
   }
}
