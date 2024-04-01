package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.EntryEvent
import com.hazelcast.map.IMap
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryRemovedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.query.Predicate
import com.orbitalhq.schemas.RemoteOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lang.taxi.query.TaxiQLQueryString
import mu.KotlinLogging
import java.util.EnumSet
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class HazelcastStreamInvoker : BaseHazelcastReadInvoker() {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun buildFlowOfCriteriaSearch(
      taxiQlQueryString: TaxiQLQueryString,
      operation: RemoteOperation,
      map: IMap<Any, Any>,
      predicate: Predicate<Any, Any>,
   ): Pair<Flow<Any?>, Int> {
      logger.debug { "Query of $taxiQlQueryString converted to criteria stream against map ${map.name} with predicate of $predicate" }
      val listener = buildListener(map, predicate, null, operation)
      return listener.flow to -1
   }

   override fun buildFlowById(
      taxiQlQueryString: TaxiQLQueryString,
      idLookupValue: Any,
      map: IMap<Any, Any>,
      operation: RemoteOperation,
   ): Pair<Flow<Any?>, Int> {
      logger.debug { "Query of $taxiQlQueryString converted to stream of single item entry listener against map ${map.name} for key $idLookupValue" }
      val listener = buildListener(map, null, idLookupValue, operation)
      return listener.flow to -1
   }

   override fun buildFlowOfFullMap(
      map: IMap<Any, Any>,
      operation: RemoteOperation,
      taxiQlQueryString: TaxiQLQueryString?,
   ): Pair<Flow<Any?>, Int> {
      logger.debug { "Query of $taxiQlQueryString converted to stream of full map against map ${map.name}" }
      val listener = buildListener(map, null, null, operation)
      return listener.flow to -1
   }


   private fun buildListener(
      map: IMap<Any, Any>,
      predicate: Predicate<Any, Any>?,
      key: Any?,
      operation: RemoteOperation,
   ): StreamingEntryListener {
      val requiredEventTypes = getRequiredEventTypes(operation)
      val listenerUUID = AtomicReference<UUID?>(null);
      val cleanupHandler = {
         val uuid = listenerUUID.get()
         if (uuid == null) {
            logger.warn { "Attempting to clean up listener attached to hazelcast map, but no UUID was found" }
         } else {
            map.removeEntryListener(uuid)
            logger.debug { "Removed Hazelcast map listener $uuid" }
         }
      }
      val listener = StreamingEntryListener( requiredEventTypes, cleanupHandler)

      val uuid = when {
         predicate != null -> map.addEntryListener(listener, predicate, true)
         key != null -> map.addEntryListener(listener, key, true)
         else -> map.addEntryListener(listener, true)
      }
      logger.debug { "Hazelcast map listener $uuid created for streaming query against map ${map.name}" }
      listenerUUID.set(uuid)
      return listener
   }

   /**
    * This is a placeholder.
    * Eventually we'll support an annotation on the operation
    * to control the types of events we want to listen to.
    * eg:
    *
    * ```
    * @HazelcastStream(eventTypes = [ADDED,UPDATED])
    * stream films : Stream<Film>
    * ```
    *
    * This isn't currently implemented, but wanted to capture the design direction.
    *
    * For now, we support ADDED, UPDATED as a resonable default
    */
   private fun getRequiredEventTypes(operation: RemoteOperation): EnumSet<StreamingEntryListener.EventType> {
      return EnumSet.of(StreamingEntryListener.EventType.ADDED, StreamingEntryListener.EventType.UPDATED)
   }


}


private class StreamingEntryListener(
   val supportedEventTypes: EnumSet<EventType>,
   cleanupHandler: () -> Unit
) : EntryAddedListener<Any, Any>, EntryUpdatedListener<Any, Any>, EntryRemovedListener<Any, Any> {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   enum class EventType {
      ADDED,
      REMOVED,
      UPDATED
   }

   private val flowSink = MutableSharedFlow<Any>()
   val flow = flowSink.asSharedFlow()

   init {
      val hasStarted = AtomicBoolean(false)
      flowSink.subscriptionCount
         .onEach { count ->
            if (count == 0 && hasStarted.get()) {
               cleanupHandler.invoke()
            }
         }
   }

   private fun emitIfEnabled(eventType: EventType, value: Any) {
      if (supportedEventTypes.contains(eventType)) {
         runBlocking {
            logger.debug { "Emitting item" }
            flowSink.emit(value)
         }
      }
   }

   override fun entryAdded(event: EntryEvent<Any, Any>) {
      emitIfEnabled(EventType.ADDED, event.value)
   }

   override fun entryUpdated(event: EntryEvent<Any, Any>) {
      emitIfEnabled(EventType.UPDATED, event.value)
   }

   override fun entryRemoved(event: EntryEvent<Any, Any>) {
      emitIfEnabled(EventType.REMOVED, event.value)
   }
}
