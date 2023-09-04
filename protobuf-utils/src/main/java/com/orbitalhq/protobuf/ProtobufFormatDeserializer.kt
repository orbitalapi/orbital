package com.orbitalhq.protobuf

import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

class ProtobufFormatDeserializer : ModelFormatDeserializer {
   private val protoSchemaCache = CacheBuilder
      .newBuilder()
      .build<Type, com.squareup.wire.schema.Schema>()

   override fun parseRequired(value: Any, metadata: Metadata): Boolean = value is ByteArray

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema): Any {
      val protobufSchema = protoSchemaCache.get(type) {
         ProtobufSpecGenerator(schema).generateProtobufSchema(type)
      }
      require(value is ByteArray) { "Can only parse a ByteArray" }
      val decoded = protobufSchema.protoAdapter(type.fullyQualifiedName, true)
         .decode(value)
      return decoded
   }
}
