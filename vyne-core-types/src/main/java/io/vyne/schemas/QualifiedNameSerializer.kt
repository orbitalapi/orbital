package io.vyne.schemas

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class QualifiedNameAsStringSerializer : JsonSerializer<QualifiedName>() {
   override fun serialize(value: QualifiedName, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeString(value.parameterizedName)
   }
}

class QualifiedNameAsStringDeserializer : JsonDeserializer<QualifiedName>() {
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): QualifiedName {
      val stringValue = p.valueAsString
      return stringValue.fqn()
   }

}
