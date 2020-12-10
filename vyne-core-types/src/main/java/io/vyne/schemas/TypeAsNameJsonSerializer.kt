package io.vyne.schemas

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * Serializes the type just as a name.
 * Use this when serializing type references inside of things that aren't types
 * eg: return types of services, etc
 */
class TypeAsNameJsonSerializer : JsonSerializer<Type>() {
   override fun serialize(value: Type?, gen: JsonGenerator, serializers: SerializerProvider) {
      if (value == null) {
         return
      }
      gen.writeObject(value.name)
   }

}
