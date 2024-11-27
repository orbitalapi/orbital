package com.orbitalhq.query.streams

import com.orbitalhq.query.*
import com.orbitalhq.query.caching.AbstractMergingStateStore
import com.orbitalhq.query.caching.StateStoreConfig
import com.orbitalhq.query.caching.StateStoreProvider
import com.orbitalhq.schemas.TaxiTypeMapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import lang.taxi.types.IntersectionType
import lang.taxi.types.StreamType
import lang.taxi.types.SumType
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
         StreamType.isStream(it.type.paramaterizedName) && SumType.isSumTypeOrWrapper(it.type.taxiType.typeParameters()[0])
      }
      if (mergedStreams.isEmpty()) {
         return QueryStrategyResult(null)
      }

      require(mergedStreams.size == 1) { "Expected a single MergedStream" }
      val streamType = mergedStreams.single().type
      val streamMemberType = streamType.typeParameters[0]
      val streamMemberTaxiType: SumType = SumType.sumTypeOrNull(streamType.taxiType.typeParameters()[0])!!

      // TODO : We need a way of getting this from the parsed query (similar to how we get Cache info)
      val emitMode = when (streamMemberTaxiType) {
         is UnionType -> StateStoreProvider.EmitMode.ON_ANY_EVENT
         is IntersectionType -> StateStoreProvider.EmitMode.AFTER_ALL_EVENTS
         else -> error("Unexpected type of stream type - ${streamMemberTaxiType::class.simpleName}")
      }

      val sumTypeRequiresStateStore = streamMemberTaxiType is IntersectionType

      val stateStore =
         if ((context.queryOptions.useStateStore || sumTypeRequiresStateStore) && stateStoreProvider != null) {
            val stateStoreConfig = context.queryOptions.stateStoreConfig ?: StateStoreConfig.DEFAULT
            stateStoreProvider.getStateStore(
               stateStoreConfig,
               streamMemberTaxiType,
               schema = context.schema,
               emitMode
            )
         } else {
            logger.info { "No cache store available, so values cannot be merged.  Each item will be emitted with nulls for non-provided values" }
            null
         }
      if (sumTypeRequiresStateStore && stateStore == null) {
         error("This query requires a StateStore to execute, but none are configured")
      }

      // Our stream is a Sum Type.
      // So, for each type in the sum, ask the query context for a stream of that type.
      // eg:
      // for Stream< A | B>, we do two searches : Stream<A> and Stream<B>
      // then merge the returned flows.
      val mergedFlow = streamMemberTaxiType.types.map {
         val innerStreamQueryNode = QuerySpecTypeNode(TaxiTypeMapper.fromTaxiType(StreamType.of(it), context.schema))
         context.find(innerStreamQueryNode).results
      }
         .merge()
         .flatMapMerge { value ->
            if (stateStore != null) {
               stateStore.mergeNotNullValues(value).asFlow()
            } else {
               // We don't have a state store, so we can't do any merging.
               // However, we need to emit an instance of the SumType that the query is producing.
               // So, construct the sum type using only the value that was provided
               val singleItem =
                  AbstractMergingStateStore.buildSumTypeFromValues(streamMemberType, listOf(value), context.schema)
               flowOf(singleItem)
            }
         }
      return QueryStrategyResult(
         mergedFlow
      )
   }
}
