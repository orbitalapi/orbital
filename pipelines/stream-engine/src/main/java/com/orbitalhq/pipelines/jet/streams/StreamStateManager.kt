package com.orbitalhq.pipelines.jet.streams

import com.google.common.annotations.VisibleForTesting
import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.map.IMap
import com.hazelcast.map.MapStore
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryRemovedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.orbitalhq.pipelines.jet.api.streams.StreamJobState
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamName
import com.orbitalhq.pipelines.jet.api.streams.StreamStateWithJobStates
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks


@Configuration
class StreamStateManagerHazelcastConfig {

   companion object {
      const val STREAM_STATUS_CACHE_BEAN_NAME = "streamStateCache"
      const val STREAM_STATUS_CACHE_NAME = "streamStatus"

      const val STREAM_JOB_STATUS_CACHE_BEAN_NAME = "streamJobStateCache"
      const val STREAM_JOB_STATUS_CACHE_NAME = "streamJobStatus"

   }

   @Bean(STREAM_STATUS_CACHE_BEAN_NAME)
   fun streamStateCache(
      hazelcastInstance: HazelcastInstance
   ): IMap<StreamName, StreamStatus> {

      return hazelcastInstance
         .getMap(STREAM_STATUS_CACHE_NAME)
   }

   @Bean(STREAM_JOB_STATUS_CACHE_BEAN_NAME)
   fun streamJobStateCache(
      hazelcastInstance: HazelcastInstance
   ): IMap<String, MutableList<StreamJobStateEvent>> {

      return hazelcastInstance
         .getMap(STREAM_JOB_STATUS_CACHE_NAME)
   }
}


/**
 * Provides a way of giving long-lived state of persistent streams.
 * By default, streams start in a PAUSED state.
 * This is so that something committed to a schema doesn't accidentally get deployed.
 *
 * Streams are submitted to this state manager, which checks the backing store
 * to see what the configured state is in (which in turn creates a default if this is the first
 * time we've seen the stream).
 *
 */
@Component
class StreamStateManager(
   @Value("\${vyne.streams.initialState:PAUSED}")
   private val initialState: StreamStatus.State = StreamStatus.State.PAUSED,
   private val pipelineManager: PipelineManager,
   @VisibleForTesting
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_BEAN_NAME)
   val streamStateCache: MutableMap<String, StreamStatus>,

   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_BEAN_NAME)
   val streamJobStateCache: MutableMap<StreamName, MutableList<StreamJobStateEvent>>,
) {


   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      pipelineManager.jobStatusEvents.subscribe { (job, event) ->
         handleJobStatusEvent(job, event)
      }
   }

   fun getStreamStatusIfExists(name: String): StreamStatus? {
      return if (streamStateCache.containsKey(name)) {
         getOrCreateStreamStatus(name)
      } else null
   }

   fun getOrCreateStreamStatus(name: String): StreamStatus {
      // We can assert non-null here, as the backing store creates a default
      // status if it doesn't exist

      val status = streamStateCache[name]

      return if (status == null) {
         val initialStatus = StreamStatus(name, initialState)
         streamStateCache[name] = initialStatus
         initialStatus
      } else {
         status
      }
   }

   /**
    * Submits the stream as a Pipeline through to the pipeline
    * manager.
    *
    * If permitted by it's state (often the configured default state
    * if this is the first time it's been submitted), the stream is started,
    * otherwise it's left as a pending stream with the pipeline manager.
    */
   fun submitStream(stream: ManagedStream): PipelineSpec<StreamingQueryInputSpec, PipelineTransportSpec> {
      val spec = pipelineManager.submitStream(stream)
      logger.info { "Submitted stream ${stream.name.parameterizedName} which was assigned pipeline spec id ${spec.id}" }
      val status = getOrCreateStreamStatus(stream.name.parameterizedName)
      if (status.state == StreamStatus.State.PAUSED) {
         logger.info { "Stream ${stream.name.parameterizedName} is in state ${status.state}, so not starting" }
      } else {
         logger.info { "Triggering start of stream ${stream.name.parameterizedName}" }
         pipelineManager.startPipeline(spec)
      }
      return spec
   }

   private fun handleJobStatusEvent(job: Job, event: StreamJobStateEvent) {
      val jobName = job.name
      val (newState, description) = event
      if (jobName == null) {
         logger.error { "Jet engine reported job with id ${job.id} changed to state $newState but not sure which job this relates to, as the originating job didn't have a name" }
         return
      }
      streamJobStateCache.compute(jobName) { _, status ->
         val statusList = status ?: mutableListOf()
         statusList.add(event)
         statusList
      }
      logger.info { "Updated stream state for ${job.name} to $newState" }
   }

   /**
    * Updates the stream state, and persists to the backing store.
    * This can also have the effect of starting / pausing the stream.
    */
   fun setStreamState(name: StreamName, state: StreamStatus.State): StreamStatus {
      val streamStatus = StreamStatus(name, state)
      // Set the state in the cache.
      // This will trigger the event listener on the appropriate node on the cluster
      // to start / stop the pipeline with the pipeline manager
      streamStateCache.set(name, streamStatus)
      logger.info { "Stream $name state updated to $streamStatus" }
      return streamStatus
   }

   fun removeStreamState(name: String) {
      streamStateCache.remove(name)
   }

   fun getAllStreamStates(): List<StreamStatus> {
      return streamStateCache.values.toList()
   }
   fun getStreamAndJobStates(): Map<StreamName, StreamStateWithJobStates> {
      return StreamStateMapListener.buildStreamAndJobStates(
         streamStateCache,
         streamJobStateCache
      )
   }
}


interface StreamStateUpdatesPublisher {
   val stateUpdates: Flux<Map<String, StreamStateWithJobStates>>
}

@Component
class StreamStateMapListener(
   private val pipelineManager: PipelineManager,
   private val streamStateCache: IMap<String, StreamStatus>,
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_BEAN_NAME)
   val streamJobStateCache: IMap<StreamName, MutableList<StreamJobStateEvent>>,
) :
   EntryAddedListener<String, StreamStatus>,
   EntryRemovedListener<String, StreamStatus>,
   EntryUpdatedListener<String, StreamStatus>,
   StreamStateUpdatesPublisher {

   private val stateUpdatesSink = Sinks.many().replay().latest<Map<String, StreamStateWithJobStates>>()
   override val stateUpdates: Flux<Map<String, StreamStateWithJobStates>> = stateUpdatesSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}

      fun buildStreamAndJobStates(
         streamStateCache: Map<String, StreamStatus>,
         jobStateEvents: Map<StreamName, MutableList<StreamJobStateEvent>>
      ): Map<StreamName, StreamStateWithJobStates> {
         val currentStreamStates = streamStateCache.values

         val streamStateWithJobStates = currentStreamStates.map { streamState ->
            val streamName = streamState.streamName
            val jobStatusesForStream = jobStateEvents[streamName]?.groupBy { it.jobId } ?: emptyMap()
            val streamJobState = jobStatusesForStream.map { (jobId, events) -> StreamJobState(jobId, events) }
            streamState.streamName to StreamStateWithJobStates(
               streamState,
               streamJobState
            )
         }.toMap()
         return streamStateWithJobStates
      }
   }

   init {
      emitCurrentState()
      streamStateCache.addEntryListener(this, true)
      val addedListener = EntryAddedListener<String, StreamStatus> { emitCurrentState() }
      val changeListener = EntryUpdatedListener<String, StreamStatus> { emitCurrentState() }
      streamJobStateCache.addEntryListener(addedListener, false)
      streamJobStateCache.addEntryListener(changeListener, false)
   }

   private fun emitCurrentState() {
      val streamStateWithJobStates = buildStreamAndJobStates(
         streamStateCache,
         streamJobStateCache
      )

      stateUpdatesSink.tryEmitNext(streamStateWithJobStates)
   }

   override fun entryAdded(event: EntryEvent<String, StreamStatus>) {
      if (event.value.state == StreamStatus.State.RUNNING && event.oldValue.state != StreamStatus.State.RUNNING && event.member.localMember()) {
         val streamName = event.value.streamName
         logger.info { "Stream $streamName entered state ${event.value.state}, so submitting to pipeline manager" }
         pipelineManager.startPipelineByName(streamName.fqn())

      }
      // Report the status of the new stream, otherwise its state is reported as UNKNOWN in the UI.
      emitCurrentState()
   }

   override fun entryRemoved(event: EntryEvent<String, StreamStatus>) {
      // I think it's already been removed from the pipeline manager at this point
      logger.info { "${event.key} removed from Stream Status cache" }
   }

   override fun entryUpdated(event: EntryEvent<String, StreamStatus>) {
      val value = event.value
      if (value.state == event.oldValue.state) {
         return
      }
      val streamName = value.streamName.fqn()
      if (event.member.localMember()) {
         when (value.state) {
            StreamStatus.State.PAUSED -> {
               logger.info { "Stream $streamName entered state ${event.value.state}, so pausing on pipeline manager" }
               pipelineManager.suspendPipelineByName(streamName)
            }

            StreamStatus.State.RUNNING -> {
               logger.info { "Stream $streamName entered state ${event.value.state}, so submitting to pipeline manager" }
               pipelineManager.startPipelineByName(streamName)
            }
         }
      }
      emitCurrentState()
   }
}

interface StreamStatusRepository : JpaRepository<StreamStatus, String>



class StreamStatusMapStore(
   private val repository: StreamStatusRepository,

   // We don't need a transaction manager.
   // However, at the time of building, the spring context was getting into a deadlock
   // when calling loadAllKeys, which deferred into the DefaultSingletonBeanRegistry::getSingleton,
   // which was hanging forever.
   // The bean it was looking for was the transactionManager.
   // So, by hoisting it here, I guess it forces creation in a different order, and the
   // method calls work.
   private val unusedTransactionManager: TransactionManager,
) : MapStore<String, StreamStatus> {
   init {
      println()
   }

   override fun load(key: String): StreamStatus? {
      return repository.findByIdOrNull(key)
   }

   override fun loadAll(keys: MutableCollection<String>): MutableMap<String, StreamStatus> {
      return repository.findAllById(keys)
         .associateBy { it.streamName }
         .toMutableMap()
   }

   override fun loadAllKeys(): MutableIterable<String> {
      val records = repository.findAll()
      return records.map { it.streamName }
         .toMutableList()
   }

   override fun deleteAll(keys: MutableCollection<String>) {
      repository.deleteAllById(keys)
   }

   override fun delete(key: String) {
      repository.deleteById(key)
   }

   override fun storeAll(map: MutableMap<String, StreamStatus>) {
      repository.saveAll(map.values)
   }

   override fun store(key: String, value: StreamStatus) {
      repository.save(value)
   }

}
