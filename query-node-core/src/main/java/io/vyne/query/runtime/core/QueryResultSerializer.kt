package io.vyne.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.format.FirstTypedInstanceInfo
import io.vyne.models.format.ModelFormatSpec
import io.vyne.models.serde.toSerializable
import io.vyne.query.QueryResult
import io.vyne.query.QueryResultSerializer
import io.vyne.query.ValueWithTypeName
import io.vyne.schemas.QueryOptions
import io.vyne.schemas.Type
import org.springframework.http.MediaType

class RawResultsSerializer(queryOptions: QueryOptions) : QueryResultSerializer {

   // TODO : We create a new object mapper for each instance of this serializer,
   // as we mutate it.  Is that expensive? Not sure.  But we can't have a shared instance.
   private val queryOptionsConverter: ObjectMapper? = queryOptions.newObjectMapperIfRequired()

   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      val converted = converter.convert(item)

      // If we need to use a special mapper (eg., to exclude nulls),
      // then convert the item using the converter before returning it.
      return if (queryOptionsConverter != null) {
         queryOptionsConverter.convertValue(converted, Any::class.java)
      } else {
         converted
      }
   }
}

class ModelFormatSpecSerializer(
   private val modelFormatSpec: ModelFormatSpec,
   private val metadata: io.vyne.schemas.Metadata
) : QueryResultSerializer {
   private var metadataEmitted: Boolean = false
   override fun serialize(item: TypedInstance): Any? {
      return if (!metadataEmitted) {
         metadataEmitted = true
         modelFormatSpec.serializer.write(item, metadata, FirstTypedInstanceInfo)
      } else {
         modelFormatSpec.serializer.write(item, metadata)
      }

   }
}

class SerializedTypedInstanceSerializer(private val contentType: String?) : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      item.toSerializable()
      return when (contentType) {
         MediaType.APPLICATION_JSON_VALUE -> converter.convert(item)
         MediaType.APPLICATION_CBOR_VALUE -> item.toSerializable().toBytes()
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

   private var metadataEmitted: Boolean = false
   override fun serialize(item: TypedInstance): Any {
      val convertedValue = if (queryOptions.requiresCustomMapper) {
         // The custom mapper will apply query-specific options (such as omitting nulls)
         mapper.convertValue(converter.convert(item), Any::class.java)
      } else {
         converter.convert(item)
      }
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
