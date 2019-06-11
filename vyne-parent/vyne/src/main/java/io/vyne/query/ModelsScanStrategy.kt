package io.vyne.query

import io.vyne.schemas.Type
import org.springframework.stereotype.Component

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
@Component
class ModelsScanStrategy : QueryStrategy {
   override fun invoke(target: Set<QuerySpecTypeNode>, context: QueryContext): QueryStrategyResult {
      return context.startChild(this, "scan for matches", OperationType.LOOKUP) { operation ->
         val targetTypes: Map<Type, QuerySpecTypeNode> = target.associateBy { it.type }

         // This is wrong, and won't work long-term
         // We need more context in order to be able to search
         // Eg., given an instance of Money, it's concievable that there would be multiple instances
         // within the graph
         val matches = targetTypes.filter { (type, querySpec) -> context.hasFactOfType(type, querySpec.mode.discoveryStrategy()) }
            .map { (type, querySpec) -> querySpec to context.getFact(type, querySpec.mode.discoveryStrategy()) }
            .toMap()
         QueryStrategyResult(matches)
      }
   }
}

fun QueryMode.discoveryStrategy(): FactDiscoveryStrategy {
   return when (this) {
      QueryMode.DISCOVER -> FactDiscoveryStrategy.ANY_DEPTH_EXPECT_ONE_DISTINCT
      QueryMode.GATHER -> FactDiscoveryStrategy.ANY_DEPTH_ALLOW_MANY
   }
}
