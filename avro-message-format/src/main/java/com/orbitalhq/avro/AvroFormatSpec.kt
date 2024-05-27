package com.orbitalhq.avro

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import lang.taxi.generators.avro.AvroMessageAnnotation
import java.time.Duration

typealias AvroSchemaCache = LoadingCache<Pair<Type, Schema>, org.apache.avro.Schema>
object AvroFormatSpec : ModelFormatSpec {
   private val avroSchemaCache: AvroSchemaCache = newSchemaCache()

   fun newSchemaCache() = CacheBuilder
      .newBuilder()
      .expireAfterAccess(Duration.ofHours(1))
      .build(object : CacheLoader<Pair<Type, Schema>, org.apache.avro.Schema>() {
         override fun load(key: Pair<Type, Schema>): org.apache.avro.Schema {
            return AvroSchemaGenerator(key.second).generateAvroSchema(key.first)
         }
      })

   const val AVRO_MEDIA_TYPE = "application/*+avro"
   override val serializer: ModelFormatSerializer = AvroFormatSerializer(avroSchemaCache)
   override val deserializer: ModelFormatDeserializer = AvroFormatDeserializer(avroSchemaCache)

   override val annotations: List<QualifiedName> = listOf(AvroMessageAnnotation.NAME.fqn())
   override val mediaType: String = AVRO_MEDIA_TYPE
}
