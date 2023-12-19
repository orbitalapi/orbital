package com.orbitalhq.query.streams

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.*
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.query.graph.operationInvocation.OperationInvocationService
import com.orbitalhq.schemas.TaxiTypeMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.types.StreamType
import lang.taxi.types.UnionType
import mu.KotlinLogging


class StreamMergingQueryStrategy(
   private val stateStoreProvider: StateStoreProvider?
) : QueryStrategy {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints
   ): QueryStrategyResult {
      val mergedStreams = target.filter {
         StreamType.isStream(it.type.paramaterizedName) && UnionType.isUnionType(it.type.taxiType.typeParameters()[0])
      }
      if (mergedStreams.isEmpty()) {
         return QueryStrategyResult(null)
      }
      require(mergedStreams.size == 1) { "Expected a single MergedStream " }
      val streamType = mergedStreams.single().type
      val streamMemberType = streamType.typeParameters[0]
      val streamMemberTaxiType: UnionType = streamType.taxiType.typeParameters()[0] as UnionType

      val connectionName = context.queryOptions.stateStoreConnectionName
      "" // TODO : We need a way of getting this from the parsed query (similar to how we get Cache info)
      val stateStore = if (stateStoreProvider != null && connectionName != null) {
         stateStoreProvider.getCacheStore(
            connectionName,
            "StreamMerge_${streamMemberTaxiType.qualifiedName}",
            schema = context.schema
         )
      } else {
         logger.info { "No cache store available, so values cannot be merged.  Each item will be emitted with nulls for non-provided values" }
         null
      }
      // Our stream is a Union Type.
      // So, for each type in the union, ask the query context for a stream of that type.
      // eg:
      // for Stream< A | B>, we do two searches : Stream<A> and Stream<B>
      // then merge the returned flows.
      val mergedFlow = streamMemberTaxiType.types.map {
         val innerStreamQueryNode = QuerySpecTypeNode(TaxiTypeMapper.fromTaxiType(StreamType.of(it), context.schema))
         context.find(innerStreamQueryNode).results
      }
         .merge()
         .map {
            val typedInstance = TypedInstance.from(streamMemberType, it, context.schema)
            typedInstance
         }.flatMapMerge { value ->
            stateStore?.mergeNotNullValues(value)?.asFlow() ?: flowOf(value)
         }
      return QueryStrategyResult(
         mergedFlow
      )
   }
}
