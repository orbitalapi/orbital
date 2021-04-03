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
      val typeName: QualifiedName?,
      val anonymousTypes: Set<Type> = emptySet(),
      /**
       * This is the serialized instance, as converted by a RawObjectMapper.
       */
      val value: Any?
   )

   override fun serialize(item: TypedInstance): Any {
      // NOte: There's a small race condition here, where we could emit metadata more than once,
      // but we don't really care,
      // and whatever we did to overcome it adds more complexity than the saved bytes are worth
      return if (!metadataEmitted) {
         metadataEmitted = true
         ValueWithTypeName(
            item.type.name,
            response.anonymousTypes,
            converter.convert(item)
         )
      } else {
         ValueWithTypeName(
            null, emptySet(),
            converter.convert(item)
         )
      }
   }

}
