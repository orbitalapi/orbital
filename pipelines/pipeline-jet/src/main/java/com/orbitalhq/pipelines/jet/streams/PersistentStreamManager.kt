package com.orbitalhq.pipelines.jet.streams

import com.google.common.collect.Maps
import com.orbitalhq.pipelines.jet.api.RunningPipelineSummary
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.toVyneQualifiedName
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.toFlux
import java.util.concurrent.ConcurrentHashMap

/**
 * Watches the schema, and creates / destroys persistent streams
 * based on those saved in the schema
 */
@Component
class PersistentStreamManager(
   private val schemaStore: SchemaChangedEventProvider,
   private val pipelineManager: PipelineManager
) {
   private val managedStreams = ConcurrentHashMap<QualifiedName, ManagedStream>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      logger.info { "Stream manager is active, monitoring schema for persistent streams" }
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
      val updatedStreamState = getStreams(schema)
      val updatedStreamQueries: Map<QualifiedName, TaxiQLQueryString> = updatedStreamState.mapValues { (_,taxiQlQuery) -> taxiQlQuery.source }

      val currentManagedStreams = pipelineManager.getManagedStreams(includeCancelled = false)
      val currentStreamQueries: Map<QualifiedName, TaxiQLQueryString> = currentManagedStreams
         .filter { it.pipeline != null }.associate { runningPipeline ->
            val pipelineSpec = runningPipeline.pipeline!!.spec
            val querySpec = pipelineSpec.input as StreamingQueryInputSpec
            pipelineSpec.name.fqn() to querySpec.query
         }


      val difference = Maps.difference(updatedStreamQueries, currentStreamQueries)

      val addedStreams = difference.entriesOnlyOnLeft()
      handleStreamsAdded(addedStreams.mapValues { entry -> updatedStreamState[entry.key]!! })

      val removedStreams = difference.entriesOnlyOnRight()
      handleStreamsRemoved(removedStreams.keys, currentManagedStreams)

      val updatedStreams = difference.entriesDiffering()
      handleStreamsUpdated(updatedStreams.mapValues { entry -> updatedStreamState[entry.key]!! })

   }

   private fun handleStreamsUpdated(updatedStreams: Map<QualifiedName, TaxiQlQuery>) {
   }

   private fun handleStreamsRemoved(
      removedStreams: MutableSet<QualifiedName>,
      currentManagedStreams: List<RunningPipelineSummary>
   ) {
      removedStreams.map { removedStreamName -> currentManagedStreams.single { runningPipeline -> runningPipeline.pipeline!!.name ==  removedStreamName.parameterizedName } }
         .forEach { runningPipelineSummary ->
            logger.info { "Managed stream ${runningPipelineSummary.pipeline!!.name} has been removed from the schema, so terminating associated stream" }
            pipelineManager.terminatePipeline(runningPipelineSummary.pipeline!!.pipelineSpecId)
         }

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
