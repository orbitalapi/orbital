package com.orbitalhq.serde

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.orbitalhq.schemas.asVyneTypeReference
import com.orbitalhq.utils.log
import lang.taxi.TaxiDocument
import lang.taxi.TaxiParser
import lang.taxi.query.FactValue
import lang.taxi.types.Type
import lang.taxi.types.TypedValue

object TaxiJacksonModule : SimpleModule("Taxi") {
   init {
      addSerializer(TaxiTypeAsVyneQualifiedNameSerializer())
      addSerializer(TaxiDocumentNoopSerializer())
      addSerializer(TaxiDocumentContextNoopSerializer())
   }

   override fun setupModule(context: SetupContext) {
      super.setupModule(context)
      context.setMixInAnnotations(FactValue::class.java, FactValueMixin::class.java)
   }

   abstract class FactValueMixin {
      @get:JsonIgnore
      abstract val typedValue: TypedValue

      @get:JsonIgnore
      abstract val variableName: String
   }
}

class TaxiDocumentContextNoopSerializer :
   StdSerializer<TaxiParser.DocumentContext>(TaxiParser.DocumentContext::class.java) {
   override fun serialize(value: TaxiParser.DocumentContext?, gen: JsonGenerator?, provider: SerializerProvider?) {
      log().warn("Not serializing document context")
   }

}

class TaxiDocumentNoopSerializer : StdSerializer<TaxiDocument>(TaxiDocument::class.java) {
   override fun serialize(value: TaxiDocument?, gen: JsonGenerator?, provider: SerializerProvider?) {
      log().warn("not serializing taxi document")
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
