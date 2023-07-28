package io.vyne

import com.google.common.collect.HashMultimap
import io.vyne.models.TypedInstance
import io.vyne.models.facts.FactBag
import io.vyne.schemas.Schema

// TODO : Replace this with a Map<FactSetId,FactBag>
typealias FactSetMap = HashMultimap<FactSetId, TypedInstance>

fun FactSetMap.toFactBag(schema:Schema, factSetIds: Set<FactSetId> = setOf(FactSets.ALL)):FactBag {
   val factset = retainFactsFromFactSet(factSetIds)
   return FactBag.of(factset.values().toList(), schema)
}
fun FactSetMap.retainFactsFromFactSet(factSetIds: Set<FactSetId>): FactSetMap {

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

