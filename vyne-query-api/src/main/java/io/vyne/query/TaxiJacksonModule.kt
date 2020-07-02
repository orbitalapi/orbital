package io.vyne.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.vyne.schemas.asVyneTypeReference
import lang.taxi.types.Type

class TaxiJacksonModule : SimpleModule("Taxi") {
   init {
      addSerializer(TaxiTypeAsVyneQualifiedNameSerializer())
   }
}

class TaxiTypeAsVyneQualifiedNameSerializer : StdSerializer<lang.taxi.types.Type>(Type::class.java) {
   override fun serialize(value: Type?, gen: JsonGenerator, serializers: SerializerProvider) {
      if (value == null) {
         return
      }
      gen.writeObject(value.asVyneTypeReference().name)
   }

}
