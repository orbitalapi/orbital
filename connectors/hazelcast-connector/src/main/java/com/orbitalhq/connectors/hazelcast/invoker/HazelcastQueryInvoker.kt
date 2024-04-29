package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.map.IMap
import com.hazelcast.query.Predicate
import com.orbitalhq.schemas.RemoteOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging

class HazelcastQueryInvoker : BaseHazelcastReadInvoker() {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun buildFlowOfCriteriaSearch(
      taxiQlQueryString: TaxiQLQueryString,
      operation: RemoteOperation,
      map: IMap<Any, Any>,
      predicate: Predicate<Any, Any>,
   ): Pair<Flow<Any?>, Int> {
      logger.debug { "Query of $taxiQlQueryString converted to criteria fetch against map ${map.name} with predicate of $predicate" }
      val results = map.values(predicate)
      return flowOf(results) to results.size
   }

   override fun buildFlowById(
      taxiQlQueryString: TaxiQLQueryString,
      idLookupValue: Any,
      map: IMap<Any, Any>,
      operation: RemoteOperation,
   ): Pair<Flow<Any?>, Int> {
      val lookupValue = map[idLookupValue]
      val recordCount = if (lookupValue == null) 0 else 1
      return if (recordCount > 0) {
         flowOf(lookupValue) to recordCount
      } else {
         flow<Any> { error("Map ${map.name} does not contain a record with key $idLookupValue") } to 0
      }
   }

   override fun buildFlowOfFullMap(
      map: IMap<Any, Any>,
      operation: RemoteOperation,
      taxiQlQueryString: TaxiQLQueryString?,
   ): Pair<Flow<Any?>, Int> {
      logger.debug { "Query of $taxiQlQueryString converted to full map fetch against map ${map.name}" }
      return flowOf(map.values) to map.size
   }
}

class MapEntryNotPresentException(val mapName: String, val key: Any) :
   RuntimeException("Failed to read from Hazelcast map $mapName - no entry with $key exists")
