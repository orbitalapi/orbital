package io.vyne.queryService

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResult
import io.vyne.query.ResultMode
import io.vyne.schemas.QualifiedName

fun ResultMode.buildSerializer(queryResponse: QueryResult): QueryResultSerializer {
   return when (this) {
      ResultMode.RAW -> RawResultsSerializer
      ResultMode.SIMPLE -> FirstEntryMetadataResultSerializer(queryResponse)
      ResultMode.VERBOSE -> TypeNamedInstanceSerializer
   }
}

interface QueryResultSerializer {
   fun serialize(item: TypedInstance): Any?
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
class FirstEntryMetadataResultSerializer(private val response: QueryResult, private val mapper: ObjectMapper = Jackson.defaultObjectMapper) : QueryResultSerializer {
   private val converter = TypedInstanceConverter(RawObjectMapper)
   private var metadataEmitted: Boolean = false

   /**
    * This differs from the TypeNamedInstance in that it's more lightweight.
    * We only include the type data occasionally (ie., the first time we send a result)
    * and after that, it's raw data.
    * This is experimental, as this approach may cause issues with polymorphic types.  However,
    * let's cross that bridge later.
    */
   data class ValueWithTypeName(
      val typeName: String?,
      /**
       * Contains the anonymous types used in this type definition.
       * Ideally, this would be typed as Set<Type>, however our Type objects are designed
       * to write TO json, not read FROM json.
       *
       * This causes problems when we're trying to rebuild ValueWithTypeName from a db
       * record.
       *
       * As the intended use of the ValueWithTypeName is on the client (web), just store
       * the types as raw JSON.
       */
      @JsonRawValue
      val anonymousTypes: String,
      /**
       * This is the serialized instance, as converted by a RawObjectMapper.
       */
      val value: Any?,
      /**
       * An id for the value - normally the hash of the originating typedInstance.
       * We need to use this so that we can look up the rich typed instance
       * later to power lineage features etc.
       * Note that even TypedNull has a hashcode, so should be ok.
       * It's possible we'll get hash collisions, so will need to tackle that
       * bridge later - though if two TypedInstances in a query result generate
       * the same hashCode, it's probably ok to use their lineage interchangably.
       */
      val valueId: Int,

      /**
       * When this instance has been generated as a direct result of a query,
       * this queryId is populated.
       */
      val queryId: String? = null
   ) {
      constructor(typeName: QualifiedName?, anonymousTypes: String = NO_ANONYMOUS_TYPES, value: Any?, valueId: Int, queryId: String?) : this(
         typeName?.parameterizedName, anonymousTypes, value, valueId, queryId
      )

      constructor(anonymousTypes: String, value: Any?, valueId: Int, queryId: String?) : this(
         null as String?,
         anonymousTypes,
         value,
         valueId,
         queryId
      )

      companion object {
         const val NO_ANONYMOUS_TYPES = "[]" // empty array
      }
   }

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
