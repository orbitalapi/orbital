package com.orbitalhq.models.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.DeferredTypedInstance

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
   fun newObjectMapperWithDefaults() =
      jacksonObjectMapper()
         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false)
         .registerModule(JavaTimeModule())
         .registerModule(SimpleModule().addSerializer(DeferredTypedInstanceSerializer()))

   val defaultObjectMapper: ObjectMapper = newObjectMapperWithDefaults()

   /**
    * An object writer intended for outputting objects in a TaxiQL query,
    * which is basically just JSON, but without quoted names.
    */
   val taxiQlObjectWriter = newObjectMapperWithDefaults()
      .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false)
}


/**
 * DeferredTypedInstances are typed instances that contain a reference to a lambda, which we
 * then evaluate.  To serialize the lambda can pull in the entire Type tree in Taxi, which
 * is way too heavy.  Using a custom serializer here.
 */
class DeferredTypedInstanceSerializer : StdSerializer<DeferredTypedInstance>(DeferredTypedInstance::class.java) {
   override fun serialize(value: DeferredTypedInstance, gen: JsonGenerator, provider: SerializerProvider?) {
      gen.writeStartObject()
      gen.writeStringField("comment", "DeferredTypedInstance omitted from serialization")
      gen.writeStringField("typeName", value.type.qualifiedName.parameterizedName)
      gen.writeStringField("dataSourceId", value.source.id)
      gen.writeStringField("value", value.expression.asTaxi())
      gen.writeEndObject()
   }

}
