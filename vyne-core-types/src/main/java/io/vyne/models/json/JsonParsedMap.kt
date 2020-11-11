package io.vyne.models.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue


interface JsonParsedStructure {
   val source: String
   val mapper: ObjectMapper
   val jsonNode: JsonNode

   companion object {
      fun from(source: String, mapper: ObjectMapper): JsonParsedStructure {
         val mapped = mapper.readValue<Any>(source)
         return when (mapped) {
            is Map<*, *> ->  JsonParsedMap(source, mapped as Map<String, Any>, mapper)
            is List<*> ->  JsonParsedList(source, mapped as List<Any>, mapper)
            else -> error("Unhandled type from parsing json - found ${mapped::class}")
         }
      }
   }
}

/**
 * Wrapper class which allows accessing the parsed content of json as a map,
 * while still allowing access to the original source.
 * Generally, whilst parsing, Map access is sufficient.
 * However, for advanced cases (like complex JsonPath), we need access to the original JsonNode.
 * This class provides for both.
 */
data class JsonParsedMap(override val source: String, val map: Map<String, Any>, override val mapper: ObjectMapper) : Map<String, Any> by map, JsonParsedStructure {
   override val jsonNode: JsonNode by lazy {
      mapper.readTree(source)
   }
}

/**
 * Wrapper class which allows accessing the parsed content of json as a list,
 * while still allowing access to the original source.
 * Generally, whilst parsing, List access is sufficient.
 * However, for advanced cases (like complex JsonPath), we need access to the original JsonNode.
 * This class provides for both.
 */
data class JsonParsedList(override val source: String, val list: List<Any>, override val mapper: ObjectMapper) : List<Any> by list, JsonParsedStructure {
   override val jsonNode: JsonNode by lazy {
      mapper.readTree(source)
   }
}
