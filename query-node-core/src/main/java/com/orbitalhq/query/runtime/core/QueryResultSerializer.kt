package com.orbitalhq.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.orbitalhq.models.RawObjectMapper
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedInstanceConverter
import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.QueryResultSerializer
import com.orbitalhq.query.ValueWithTypeName
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import org.springframework.http.MediaType
import java.util.concurrent.atomic.AtomicInteger

class RawResultsSerializer(
   queryOptions: QueryOptions,
   /**
    * Indicates if we should serialize this out directly
    * as JSON, or leave that to the Spring layer.
    * For legacy reasons, this was false, but
    * to be consistent with other QueryResultSerializers,
    * we should probably return the actual JSON.
    */
   private val returnJson: Boolean = false,
   private val usePrettyPrinting: Boolean = false,
   private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : QueryResultSerializer {

   override val contentType: String = MediaType.APPLICATION_JSON_VALUE
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance, schema: Schema): Any? {
      val valueToSerialize = converter.convert(item)
      return if(returnJson) {
         if (usePrettyPrinting) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueToSerialize)
         } else {
            objectMapper.writeValueAsString(valueToSerialize)
         }

      } else {
         valueToSerialize
      }
   }
}


class ModelFormatSpecSerializer(
   private val modelFormatSpec: ModelFormatSpec,
   private val metadata: com.orbitalhq.schemas.Metadata
) : QueryResultSerializer {
   private var index = AtomicInteger(-1)
   override fun serialize(item: TypedInstance, schema: Schema): Any? {
      return modelFormatSpec.serializer.write(item, metadata, schema, index.incrementAndGet())
   }

   override val contentType: String = modelFormatSpec.mediaType
}

class SerializedTypedInstanceSerializer(override val contentType: String) : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance, schema: Schema): Any? {
      item.toSerializable()
      return when (contentType) {
         "application/json" -> converter.convert(item)
         "application/cbor" -> item.toSerializable().toBytes()
         else -> throw IllegalArgumentException("Unsupported content type: $contentType")
      }
   }
}

/**
 * QueryResultSerializer which includes type metadata on the first emitted entry only.
 * After that, metadata is left empty.
 * Used for serializing results to the UI
 */
class FirstEntryMetadataResultSerializer(
   private val anonymousTypes: Set<Type> = emptySet(),
   private val queryId: String? = null,
   private val queryOptions: QueryOptions
) :
   QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   private val mapper = queryOptions.newObjectMapper()

   override val contentType: String = MediaType.APPLICATION_JSON_VALUE

   private var metadataEmitted: Boolean = false
   override fun serialize(item: TypedInstance, schema: Schema): Any {
      val convertedValue = converter.convert(item)
      // NOte: There's a small race condition here, where we could emit metadata more than once,
      // but we don't really care,
      // and whatever we did to overcome it adds more complexity than the saved bytes are worth

      return if (!metadataEmitted) {
         metadataEmitted = true
         ValueWithTypeName(
            item.type.name,
            mapper.writeValueAsString(anonymousTypes),
            convertedValue,
            valueId = item.hashCodeWithDataSource,
            queryId = queryId
         )
      } else {
         ValueWithTypeName(
            "[]",
            convertedValue,
            valueId = item.hashCodeWithDataSource,
            queryId = queryId
         )
      }
   }

   companion object {
      fun forQueryResult(result: QueryResult, options: QueryOptions) =
         FirstEntryMetadataResultSerializer(result.anonymousTypes, result.queryId, options)
   }
}
