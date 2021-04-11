package io.vyne.queryService

import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.query.QueryResult
import io.vyne.query.ResultMode
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type

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
class FirstEntryMetadataResultSerializer(private val response: QueryResult) : QueryResultSerializer {
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
      val anonymousTypes: Set<Type> = emptySet(),
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
      constructor(typeName: QualifiedName?, anonymousTypes: Set<Type> = emptySet(), value: Any?, valueId: Int, queryId: String?) : this(
         typeName?.parameterizedName, anonymousTypes, value, valueId, queryId
      )

      constructor(anonymousTypes: Set<Type>, value: Any?, valueId: Int, queryId: String?) : this(
         null as String?,
         anonymousTypes,
         value,
         valueId,
         queryId
      )
   }

   override fun serialize(item: TypedInstance): Any {
      // NOte: There's a small race condition here, where we could emit metadata more than once,
      // but we don't really care,
      // and whatever we did to overcome it adds more complexity than the saved bytes are worth
      return if (!metadataEmitted) {
         metadataEmitted = true
         ValueWithTypeName(
            item.type.name,
            response.anonymousTypes,
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = response.queryId
         )
      } else {
         ValueWithTypeName(
            emptySet(),
            converter.convert(item),
            valueId = item.hashCodeWithDataSource,
            queryId = response.queryId
         )
      }
   }

}
