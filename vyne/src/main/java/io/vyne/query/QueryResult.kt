package io.vyne.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypeNamedInstanceMapper
import io.vyne.models.TypedInstance
import io.vyne.models.TypedInstanceConverter
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map

@JsonInclude(JsonInclude.Include.NON_NULL)
data class QueryResult(
    @field:JsonIgnore
   val querySpec: QuerySpecTypeNode,
    @field:JsonIgnore // we send a lightweight version below
   val results: Flow<TypedInstance>,
    @Deprecated("Being removed, QueryResult is now just a wrapper around the results")
   @field:JsonIgnore // this sends too much information - need to build a lightweight version
   override val profilerOperation: ProfilerOperation? = null,
    @Deprecated("It's no longer possible to know at the time the QueryResult is instantiated if the query has been fully resolved.  Catch the exception from the Flow<> instead.")
   override val isFullyResolved: Boolean,
    val anonymousTypes: Set<Type> = setOf(),
    override val clientQueryId: String? = null,
    override val queryId: String,
    @field:JsonIgnore // we send a lightweight version below
   val statistics: MutableSharedFlow<VyneQueryStatistics>? = null,
    override val responseType: String? = null,

    @field:JsonIgnore
   private val onCancelRequestHandler: () -> Unit = {}
) : QueryResponse {
   override val queryResponseId: String = queryId
   val duration = profilerOperation?.duration

   @Deprecated(
      "Now that a query only reflects a single type, this does not make sense anymore",
      replaceWith = ReplaceWith("isFullyResolved")
   )
   @get:JsonIgnore // Deprecated
   val unmatchedNodes: Set<QuerySpecTypeNode> by lazy {
      setOf(querySpec)
   }
   override val responseStatus: QueryResponse.ResponseStatus = if (isFullyResolved) QueryResponse.ResponseStatus.COMPLETED else QueryResponse.ResponseStatus.INCOMPLETE

   // for UI
   val searchedTypeName: QualifiedName = querySpec.type.qualifiedName

   /**
    * Returns the result stream with all type information removed.
    */
   @get:JsonIgnore
   val rawResults: Flow<Any?>
      get() {
         val converter = TypedInstanceConverter(RawObjectMapper)
         return results.map {
            converter.convert(it)
         }
      }

   /**
    * Returns the result stream converted to TypeNamedInstances.
    * Note that depending on the actual values provided in the results,
    * we may emit TypeNamedInstance or TypeNamedInstace[].  Nulls
    * present in the result stream are not filtered.
    * For these reasons, the result is Flow<Any?>
    *
    */
   @get:JsonIgnore
   val typedNamedInstanceResults: Flow<Any?>
      get() {
         val converter = TypedInstanceConverter(TypeNamedInstanceMapper)
         return results.map { converter.convert(it) }
      }

   fun requestCancel() {
      this.onCancelRequestHandler.invoke()
   }
}
