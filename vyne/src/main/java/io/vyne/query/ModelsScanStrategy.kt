package io.vyne.query

import io.vyne.models.FactDiscoveryStrategy
import io.vyne.models.TypedInstance
import io.vyne.schemas.Type
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Scans the facts in the QueryContext to see if the desired attributes are present.
 *
 * Currently, only the top-level target nodes within @param target are scanned.
 * This is okay-ish as a first pass, as it's likely that finding a match
 * at a lower-level entity doesn't make sense.
 * (ie., matching a property of an entity, but not it's entity)
 *
 * This is a first-pass query strategy, and will likely be removed in
 * favour of something more graph-based.
 */
class ModelsScanStrategy : QueryStrategy {
   override suspend fun invoke(
      target: Set<QuerySpecTypeNode>,
      context: QueryContext,
      invocationConstraints: InvocationConstraints): QueryStrategyResult {
      val spec = invocationConstraints.typedInstanceValidPredicate
      if (context.debugProfiling) {// enable profiling via context.debugProfiling=true flag
         return context.startChild(this, "scan for matches", OperationType.LOOKUP) { operation ->
            scanForMatches(target, context, spec)
         }
      }
      return scanForMatches(target, context, spec)
   }

   private fun scanForMatches(target: Set<QuerySpecTypeNode>, context: QueryContext, spec:TypedInstanceValidPredicate): QueryStrategyResult {
      val targetTypes: Map<Type, QuerySpecTypeNode> = target.associateBy { it.type }

      // This is wrong, and won't work long-term
      // We need more context in order to be able to search
      // Eg., given an instance of Money, it's concievable that there would be multiple instances
      // within the graph
      val matches = targetTypes
         .map { (type, querySpec) -> querySpec to context.getFactOrNull(type, querySpec.mode.discoveryStrategy(), spec) }
         .filter { it.second != null }
         .filter { spec.isValid(it.second!!) }
         .toMap()
         .map { it.value }

      if (matches.isEmpty()) {
         return QueryStrategyResult(null)
      }

      return QueryStrategyResult( matches.asFlow() as Flow<TypedInstance>)
   }
}

fun QueryMode.discoveryStrategy(): FactDiscoveryStrategy {
   return when (this) {
      QueryMode.DISCOVER -> FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
      QueryMode.GATHER -> FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY
      QueryMode.BUILD -> FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
   }
}
