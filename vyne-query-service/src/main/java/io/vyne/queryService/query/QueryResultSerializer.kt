package io.vyne.queryService.query

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.format.FirstTypedInstanceInfo
import io.vyne.models.format.ModelFormatSpec
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResult
import io.vyne.query.QueryResultSerializer
import io.vyne.query.ValueWithTypeName
import io.vyne.schemas.Type
import io.vyne.spring.projection.serde.toSerializable

object RawResultsSerializer : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      return converter.convert(item)
   }
}

class ModelFormatSpecSerializer(
   private val modelFormatSpec: ModelFormatSpec,
   private val metadata: io.vyne.schemas.Metadata): QueryResultSerializer {
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

object TypeNamedInstanceSerializer : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      item.toSerializable()
      return converter.convert(item)
   }
}

/**
 * QueryResultSerializer which includes type metadata on the first emitted entry only.
 * After that, metadata is left empty.
 * Used for serializing results to the UI
 */
class FirstEntryMetadataResultSerializer(private val anonymousTypes: Set<Type> = emptySet(), private val queryId: String? = null, private val mapper: ObjectMapper = Jackson.defaultObjectMapper) :
    QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   private var metadataEmitted: Boolean = false
   override fun serialize(item: TypedInstance): Any {
      // NOte: There's a small race condition here, where we could emit metadata more than once,
      // but we don't really care,
      // and whatever we did to overcome it adds more complexity than the saved bytes are worth
      return if (!metadataEmitted) {
         metadataEmitted = true
         ValueWithTypeName(
            item.type.name,
            mapper.writeValueAsString(anonymousTypes),
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = queryId
         )
      } else {
         ValueWithTypeName(
            "[]",
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = queryId
         )
      }
   }
   companion object {
      fun forQueryResult(result: QueryResult) = FirstEntryMetadataResultSerializer(result.anonymousTypes, result.queryId)
   }
}
