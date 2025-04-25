package com.orbitalhq.protobuf

import com.google.common.cache.CacheBuilder
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.SchemaHash
import com.orbitalhq.schemas.Type
import java.time.Duration

private data class CacheKey(
   val typeName: String,
   val schemaHash: SchemaHash,
)
class ProtobufFormatDeserializer : ModelFormatDeserializer {
   private val protoSchemaCache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(Duration.ofHours(1))
      .build<CacheKey, com.squareup.wire.schema.Schema>()

   override fun canParse(value: Any, metadata: Metadata, type: Type): Boolean = value is ByteArray

   override fun parse(value: Any, type: Type, metadata: Metadata, schema: Schema, source: DataSource): Any {
      val cacheKey = CacheKey(type.paramaterizedName, schema.hash)
      val protobufSchema = protoSchemaCache.get(cacheKey) {
         ProtobufSpecGenerator(schema).generateProtobufSchema(type)
      }
      require(value is ByteArray) { "Can only parse a ByteArray, instead received a ${value::class.simpleName}" }
      val decoded = protobufSchema.protoAdapter(type.fullyQualifiedName, true)
         .decode(value)
      return decoded
   }
}
