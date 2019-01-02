package io.vyne

import com.google.common.collect.HashMultimap
import io.vyne.models.TypedInstance

typealias FactSetId = String
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

object FactSets {
   const val DEFAULT: FactSetId = "DEFAULT"
   const val CALLER: FactSetId = "CALLER"
   const val ALL: FactSetId = "@@ALL"
   const val NONE: FactSetId = "@@NONE"
   fun new(): FactSetMap {
      return FactSetMap.create()
   }
}
