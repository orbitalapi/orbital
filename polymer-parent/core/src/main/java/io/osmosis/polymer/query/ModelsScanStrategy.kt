package io.osmosis.polymer.query

import io.osmosis.polymer.models.TypedInstance
import io.osmosis.polymer.schemas.Type
import org.springframework.stereotype.Component
import kotlin.streams.toList

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
      val targetTypes: Map<Type, QuerySpecTypeNode> = target.associateBy { it.type }

      val matches = context.modelTree()
         .filter { typedInstance: TypedInstance -> targetTypes.containsKey(typedInstance.type) }
         .map { typedInstance -> targetTypes[typedInstance.type]!! to typedInstance }
         .toList().toMap()
      return QueryStrategyResult(matches)
   }
}
