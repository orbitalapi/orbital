package io.vyne.queryService.query

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import io.vyne.query.QueryResult
import io.vyne.query.QueryResultSerializer
import io.vyne.query.ResultMode
import io.vyne.query.SearchFailedException
import io.vyne.query.ValueWithTypeName
import io.vyne.queryService.csv.toCsv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

fun QueryResponse.convertToSerializedContent(
   resultMode: ResultMode,
   contentType: String
): Flow<Any> {
   return when (this) {
      is QueryResult -> this.convertToSerializedContent(resultMode, contentType)
      is FailedSearchResponse -> this.convertToSerializedContent(contentType)
      else -> error("Received unknown type of QueryResponse: ${this::class.simpleName}")
   }
}

private fun FailedSearchResponse.convertToSerializedContent(
   contentType: String
): Flow<Any> {
   return when (contentType) {
      TEXT_CSV -> flowOf(this.message)
      // Assume everything else is JSON.  Return the entity, and let
      // Spring / Jackson take care of the serialzation.
      else -> flowOf(this)
   }
}
fun QueryResult.convertToSerializedContent(
   resultMode: ResultMode,
   contentType: String
): Flow<Any> {
   val serializer =
      if (contentType == TEXT_CSV) ResultMode.RAW.buildSerializer(this) else resultMode.buildSerializer(
         this
      )

   return when (contentType) {
      TEXT_CSV -> toCsv(this.results, serializer)
      // Default everything else to JSON
      else -> {
         this.results
            .catch { error ->
               when (error) {
                  is SearchFailedException -> {
                     throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        error.message ?: "Search failed without a message"
                     )
                  }
                  else -> throw error
               }
            }
            .flatMapMerge { typedInstance ->
               // This is a smell.
               // I've noticed that when projecting, in this point of the code
               // we get individual typed instances.
               // However, if we're not projecting, we get a single
               // typed collection.
               // This meas that the shape of the response (array vs single)
               // varies based on the query, which is incorrect.
               // Therefore, unwrap collections here.
               // This smells, because it could be indicative of a problem
               // higher in the stack.
               if (typedInstance is TypedCollection) {
                  typedInstance.map { serializer.serialize(it) }
               } else {
                  listOf(serializer.serialize(typedInstance))
               }.filterNotNull()
                  .asFlow()

            }
            .filterNotNull()
      }
   }
}

fun ResultMode.buildSerializer(queryResponse: QueryResult): QueryResultSerializer {
   return when (this) {
      ResultMode.RAW -> RawResultsSerializer
      ResultMode.SIMPLE, ResultMode.TYPED -> FirstEntryMetadataResultSerializer(queryResponse)
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
class FirstEntryMetadataResultSerializer(private val response: QueryResult, private val mapper: ObjectMapper = Jackson.defaultObjectMapper) :
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
            mapper.writeValueAsString(response.anonymousTypes),
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = response.queryId
         )
      } else {
         ValueWithTypeName(
            "[]",
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = response.queryId
         )
      }
   }

}
