package io.vyne.models

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class TypeNamedInstanceDeserialzier : JsonDeserializer<TypeNamedInstance>() {
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TypeNamedInstance {
      val rawMap = p.readValueAs(Any::class.java)
      val deserialized = when (rawMap) {
         is Map<*, *> -> deserializeMap(rawMap as Map<Any, Any>)
         is List<*> -> {
            // Currently, type data of the list itself isn't written (ie., List<T>)
            // only the values of the list.
            // When this list is converted to a TypedInstance, we acutally look up the
            // collection type from the schema.
            // However, it's a bit nasty, and we should try to find a better solution.
            val collectionValue = rawMap.map { deserializeValue(it as Any) }
            TypeNamedInstance("vyne.deserialization.UnknownCollectionType", collectionValue)

         }
         else -> error("Unhandled scenario deserializing top level entry - expected either a map or a list, found ${rawMap::class.simpleName}")
      }
      return deserialized as TypeNamedInstance
   }

   private fun deserializeValue(value: Any): Any {
      return when (value) {
         is List<*> -> value.map { deserializeValue(it as Any) }
         is Map<*, *> -> deserializeMap(value as Map<Any, Any>)
         else -> value
      }
   }

   private fun deserializeMap(rawMap: Map<Any, Any>): Any {
      val isTypeNamedInstance = rawMap.size == 2 && rawMap.containsKey("typeName") && rawMap.containsKey("value")
      if (isTypeNamedInstance) {
         val typeName = rawMap.getValue("typeName") as String
         val value = deserializeValue(rawMap.getValue("value"))
         return TypeNamedInstance(typeName, value)
      }
      return rawMap.map { (key, value) ->
         key to deserializeValue(value)
      }.toMap()
   }

}
