package com.orbitalhq.avro

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.orbitalhq.SourcePackage
import com.orbitalhq.models.format.ModelFormatDeserializer
import com.orbitalhq.models.format.ModelFormatSerializer
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.SchemaHash
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.sourceMapFromVersionedSource
import lang.taxi.generators.SourceMap
import lang.taxi.generators.avro.AvroMessageAnnotation
import lang.taxi.packages.SourcesTypes
import lang.taxi.types.ParameterizedName
import mu.KotlinLogging
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import java.time.Duration

object AvroFormatSpec : ModelFormatSpec {
   val avroSchemaCache = AvroSchemaCache()

   const val AVRO_MEDIA_TYPE = "application/*+avro"
   override val serializer: ModelFormatSerializer = AvroFormatSerializer(avroSchemaCache)
   override val deserializer: ModelFormatDeserializer = AvroFormatDeserializer(avroSchemaCache)

   override val annotations: List<QualifiedName> = listOf(AvroMessageAnnotation.NAME.fqn())
   override val mediaType: String = AVRO_MEDIA_TYPE

   fun clearCache() {
      avroSchemaCache.clear()
   }
}


class AvroSchemaCache {
   private val cache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(Duration.ofHours(1))
      .build<SchemaHash, AvroSchemaCollection>()

   fun get(schema: Schema, type: Type): org.apache.avro.Schema {
      val schemaCollection = cache.get(schema.hash) { AvroSchemaCollection(schema) }
      return schemaCollection.getOrBuild(type)
   }

   fun clear() {
      cache.invalidateAll()
      cache.cleanUp()
   }

}

private data class AvroSchemaCollection(
   private val taxiSchema: Schema,
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   private val sourceMaps: List<Pair<SourcePackage, SourceMap>> = SourcePackage.getSourceMaps(taxiSchema)

   private val createdSchemas = ConcurrentHashMap<ParameterizedName, org.apache.avro.Schema>()
   fun getOrBuild(type: Type):org.apache.avro.Schema {
      if (type.isCollection) {
         // The sourceMap doesn't contain an entry for Foo[], just Foo.
         // So, trying to find the original schema needs to be against the collection type
         return getOrBuild(type.collectionType!!)
      }

      return createdSchemas.getOrPut(type.name.parameterizedName) {
         logger.info { "Building schema for type ${type.name.shortDisplayName}" }
         val (sourcePackage, sourceMap) = sourceMaps.firstOrNull { (_, sourceMap) ->
            sourceMap.containsType(type.paramaterizedName)
         } ?: error("Cannot find original avro schema - no source map exists for type ${type.name.shortDisplayName}")
         val schemaFileNames = sourceMap.getSourceFileNameForType(type.name.parameterizedName)
         val schemaFileName = if (schemaFileNames.size == 1) {
            schemaFileNames.single()
         } else {
            error("Avro formats expect a single source file per type, but found ${schemaFileNames.size} for type ${type.name.parameterizedName} - ${schemaFileNames.joinToString()}")
         }
         val originalSources = sourcePackage.additionalSources[SourcesTypes.ORIGINAL_SOURCE] ?: emptyList()
         val schemaFile = originalSources.firstOrNull { it.name == schemaFileName }
            ?: error("Cannot find the mapped source file ($schemaFileName) for type ${type.name.shortDisplayName} ")

         logger.info { "Found schema ${schemaFile.name} in package ${sourcePackage.identifier.id} for type ${type.name.shortDisplayName}"}
         val avroSchema = org.apache.avro.Schema.Parser().parse(schemaFile.content)
         avroSchema
      }
   }


}
