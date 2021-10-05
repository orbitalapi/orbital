package io.vyne.queryService.query

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResult
import io.vyne.query.QueryResultSerializer
import io.vyne.query.ResultMode
import io.vyne.query.ValueWithTypeName
import io.vyne.schemas.Type

fun ResultMode.buildSerializer(queryResponse: QueryResult): QueryResultSerializer {
   return when (this) {
      ResultMode.RAW -> RawResultsSerializer
      ResultMode.SIMPLE, ResultMode.TYPED -> FirstEntryMetadataResultSerializer(queryResponse.queryId, queryResponse.anonymousTypes)
      ResultMode.VERBOSE -> TypeNamedInstanceSerializer
   }
}


object RawResultsSerializer : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      return converter.convert(item)
   }
}

object TypeNamedInstanceSerializer : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   override fun serialize(item: TypedInstance): Any? {
      return converter.convert(item)
   }
}

/**
 * QueryResultSerializer which includes type metadata on the first emitted entry only.
 * After that, metadata is left empty.
 * Used for serializing results to the UI
 */
class FirstEntryMetadataResultSerializer(private val queryId: String, private val anonymousTypes: Set<Type>, private val mapper: ObjectMapper = Jackson.defaultObjectMapper) :
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

}
