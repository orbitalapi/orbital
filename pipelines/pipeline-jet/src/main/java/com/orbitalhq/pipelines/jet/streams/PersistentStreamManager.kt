package com.orbitalhq.pipelines.jet.streams

import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.log.LogLevel
import com.orbitalhq.pipelines.jet.api.transport.log.LoggingOutputSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import reactor.kotlin.core.publisher.toFlux
import java.util.concurrent.ConcurrentHashMap

/**
 * Watches the schema, and creates / destroys persistent streams
 * based on those saved in the schema
 */
class PersistentStreamManager(
   private val schemaStore: SchemaChangedEventProvider,
   private val pipelineManager: PipelineManager
) {
   private val managedStreams = ConcurrentHashMap<QualifiedName, ManagedStream>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      schemaStore.schemaChanged
         .toFlux()
         .subscribe { schemaSetChangedEvent ->
            logger.info { "Schema changed, now on generation ${schemaSetChangedEvent.newSchemaSet.generation} - checking for changes to managed streams" }
            updateStreams(schemaSetChangedEvent.newSchemaSet.schema)
         }
   }

   fun listCurrentStreams(): List<ManagedStream> {
      return managedStreams.values.toList()
   }

   private fun updateStreams(schema: Schema) {
      val updatesStreamState = getStreams(schema)
      val currentStreams = managedStreams.mapValues { it.value.query }
      val difference = Maps.difference(updatesStreamState, currentStreams)

      val addedStreams = difference.entriesOnlyOnLeft()
      handleStreamsAdded(addedStreams)

      val removedStreams = difference.entriesOnlyOnRight()
      handleStreamsRemoved(removedStreams)

      val updatedStreams = difference.entriesDiffering()
      handleStreamsUpdated(updatedStreams)

   }

   private fun handleStreamsUpdated(updatedStreams: Map<QualifiedName, MapDifference.ValueDifference<TaxiQlQuery>>) {
   }

   private fun handleStreamsRemoved(removedStreams: Map<QualifiedName, TaxiQlQuery>) {
   }

   private fun handleStreamsAdded(addedStreams: Map<QualifiedName, TaxiQlQuery>) {
      addedStreams
         .forEach {
            logger.info { "Creating a persistent stream for ${it.key}" }
            createAndSubmitStream(it.value)
         }
   }

   private fun createAndSubmitStream(query: TaxiQlQuery) {
      pipelineManager.startPipeline(ManagedStream.from(query))
   }


   // should store, and then start the stream
   private fun submitStream(managedStream: ManagedStream) {
      if (managedStream.streamType != ManagedStream.StreamType.CONTINUOUS) {
         logger.info { "Not starting a pipeline for stream ${managedStream.name} as it's type is ${managedStream.streamType}" }
         return
      }
      // TODO : Something about start
      val currentValue = managedStreams.putIfAbsent(managedStream.name, managedStream)
      if (currentValue != null) {
         logger.warn { "Attempted to submit stream ${managedStream.name}, but that stream was already present. Remove and add to update" }
      } else {
         startStream(managedStream)
      }
   }

   private fun startStream(managedStream: ManagedStream) {
      logger.info { "Starting stream ${managedStream.name}" }
      val spec = PipelineSpec<StreamingQueryInputSpec, LoggingOutputSpec>(
         managedStream.name.longDisplayName,
         StreamingQueryInputSpec(managedStream.query.source),
         null,
         listOf(LoggingOutputSpec(LogLevel.INFO, managedStream.name.longDisplayName))
      )
      pipelineManager.startPipeline(spec)
   }

   private fun getStreams(schema: Schema): Map<QualifiedName, TaxiQlQuery> {
      return schema.taxi.queries
         .filter { it.queryMode == QueryMode.STREAM }
         .associateBy { it.name.toVyneQualifiedName() }
   }
}

data class ManagedStream(
   val name: QualifiedName,
   val query: TaxiQlQuery,
   val streamType: StreamType,
   val enabled: Boolean = true,
   val state: StreamState = StreamState.NOT_STARTED
) {
   companion object {
      fun from(query: TaxiQlQuery, enabled: Boolean = true): ManagedStream {
         val streamType: ManagedStream.StreamType = detectStreamType(query)
         return ManagedStream(query.name.toVyneQualifiedName(), query, streamType, enabled)
      }

      // TODO. Placeholder.
      private fun detectStreamType(value: TaxiQlQuery): ManagedStream.StreamType = ManagedStream.StreamType.CONTINUOUS

   }

   // Capturing design thoughts, might not be fully implemented, will
   // start with contious streams first.
   enum class StreamType {
      /**
       * A stream that is always running.
       * These are streams that do not have an @HttpOperation annotation associated with them
       * Executed by our pipeline infrastrcture, which should ensure there's only ever a single instance running.
       */
      CONTINUOUS,

      /**
       * A new stream is created per request, and terminated when the requestor goes away.
       * This is for streams that contain an @HttpOperation on them.
       */
      PER_REQUEST
   }

   enum class StreamState {
      NOT_STARTED,
      SCHEDULED,
      STARTED,
      STOPPING,
      STOPPED
   }
}
