package io.vyne

import com.google.common.collect.HashMultimap
import io.vyne.models.TypedInstance

typealias FactSetMap = HashMultimap<FactSetId, TypedInstance>

fun FactSetMap.filterFactSets(factSetIds: Set<FactSetId>): FactSetMap {

   return when {
      factSetIds.contains(FactSets.ALL) -> this
      factSetIds.contains(FactSets.NONE) -> FactSetMap.create()
      else -> {
         val result: FactSetMap = FactSetMap.create()
         factSetIds.forEach { result.putAll(it, this.get(it)) }
         result
      }
   }
}

fun emptyNodesetMap(): FactSetMap {
   return HashMultimap.create()
}

