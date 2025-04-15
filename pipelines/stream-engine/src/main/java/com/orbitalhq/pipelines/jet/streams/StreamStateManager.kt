package com.orbitalhq.pipelines.jet.streams

import com.google.common.annotations.VisibleForTesting
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.Job
import com.hazelcast.map.IMap
import com.hazelcast.map.MapStore
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryUpdatedListener
import com.hazelcast.topic.ITopic
import com.orbitalhq.pipelines.jet.api.streams.StreamJobState
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamName
import com.orbitalhq.pipelines.jet.api.streams.StreamStateWithJobStates
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.query.StreamingQueryInputSpec
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig.Companion.STREAM_JOB_STATUS_CACHE_NAME
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig.Companion.STREAM_STATUS_CACHE_NAME
import com.orbitalhq.pipelines.jet.streams.StreamStateManagerHazelcastConfig.Companion.STREAM_STATUS_TOPIC_BEAN_NAME
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.fqn
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
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
import reactor.core.scheduler.Schedulers


@Configuration
class StreamStateManagerHazelcastConfig {

   companion object {
      const val STREAM_STATUS_CACHE_BEAN_NAME = "streamStateCache"
      const val STREAM_STATUS_CACHE_NAME = "streamStatus"

      const val STREAM_JOB_STATUS_CACHE_BEAN_NAME = "streamJobStateCache"
      const val STREAM_JOB_STATUS_CACHE_NAME = "streamJobStatus"

      const val STREAM_STATUS_TOPIC_BEAN_NAME = "streamStatusTopic"
   }

   @Bean(STREAM_STATUS_TOPIC_BEAN_NAME)
   fun streamStatusChangedTopic(hazelcastInstance: HazelcastInstance): ITopic<StreamStateChangeEvent> {
      // MUST BE A RELIABLE TOPIC.
      return hazelcastInstance.getReliableTopic(StreamStateChangeEvent.topicName)
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
class StreamStateManager @Autowired constructor(
   @Value("\${vyne.streams.initialState:PAUSED}")
   private val initialState: StreamStatus.State = StreamStatus.State.PAUSED,
   private val pipelineManager: PipelineManager,
   @VisibleForTesting
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_BEAN_NAME)
   val streamStateCache: MutableMap<String, StreamStatus>,

   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_BEAN_NAME)
   val streamJobStateCache: MutableMap<StreamName, MutableList<StreamJobStateEvent>>,

   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_TOPIC_BEAN_NAME)
   val streamChangeEventTopic: ITopic<StreamStateChangeEvent>
) {
   constructor(
      initialState: StreamStatus.State = StreamStatus.State.PAUSED,
      pipelineManager: PipelineManager,
      hazelcastInstance: HazelcastInstance
   ) : this(
      initialState, pipelineManager,
      hazelcastInstance.getMap(STREAM_STATUS_CACHE_NAME),
      hazelcastInstance.getMap(STREAM_JOB_STATUS_CACHE_NAME),
      hazelcastInstance.getReliableTopic(StreamStateChangeEvent.topicName)
   )


   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {
      pipelineManager
         .jobStatusEvents
         // Hopping on an elastic thread here to not block hazelcast event thread.
         // as JobStatusEvents are published from an HZ Event Listener.
         .publishOn(Schedulers.boundedElastic())
         .subscribe { (job, event) ->
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
         setStreamState(initialStatus)
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
      if (jobName == null) {
         logger.error { "Jet engine reported job with id ${job.id} changed to state $${event.status} but not sure which job this relates to, as the originating job didn't have a name" }
         return
      }
      streamJobStateCache.compute(jobName) { _, status ->
         val statusList = status ?: mutableListOf()
         statusList.add(event)
         statusList
      }
      logger.info { "Updated stream state for ${event.streamName}: $event" }
   }

   /**
    * Updates the stream state, and persists to the backing store.
    * This can also have the effect of starting / pausing the stream.
    */
   fun setStreamState(name: StreamName, state: StreamStatus.State): StreamStatus {
      val streamStatus = StreamStatus(name, state)
     return setStreamState(streamStatus)
   }
   fun setStreamState(streamStatus: StreamStatus): StreamStatus {
      val name = streamStatus.streamName
      // Set the state in the cache.
      // This will trigger the event listener on the appropriate node on the cluster
      // to start / stop the pipeline with the pipeline manager
      val oldValue = streamStateCache.put(name, streamStatus)
      logger.info { "Stream $name state updated to $streamStatus. Is change: ${oldValue != streamStatus}.  OldValue: ${oldValue}" }
      streamChangeEventTopic.publish(StreamStateChangeEvent(name, streamStatus, oldValue))
      return streamStatus
   }

   fun removeStreamState(name: String) {
      streamStateCache.remove(name)
   }

   fun getAllStreamStates(): List<StreamStatus> {
      return streamStateCache.values.toList()
   }

   fun getStreamAndJobStates(): Map<StreamName, StreamStateWithJobStates> {
      return StreamStateChangeEventListener.buildStreamAndJobStates(
         streamStateCache,
         streamJobStateCache
      )
   }
}

data class StreamStateChangeEvent(
   val name: StreamName,
   val newState: StreamStatus,
   val oldState: StreamStatus?
) {
   companion object {
      const val topicName = "StreamStateChangeEvents"
   }

}

interface StreamStateUpdatesPublisher {
   val stateUpdates: Flux<Map<String, StreamStateWithJobStates>>
}

/**
 * Listens to events that a stream has changed it's desired state, and updates the
 * Pipeline manager accordingly.
 *
 * This class is responsible for telling the PipelineManager to start and stop streams.
 *
 * The class gets notified of state changes via a ReliableTopic (see below for discussion around
 * this choice).
 * It will only act on events where the map entry for the stream is owned by the current node.
 * This allows us to distribute event updates across the cluster, and only process on a single node.
 *
 * Re: EntryListener vs ReliableTopic:
 * Originally we used an EntryListener on the StreamStatusCache. However
 * we found that this would work fine for the first few-ish events, then would stop
 * responding. Via debugging we confirmed that EntryUpdated events were being dispatched
 * into Hazelcast's event system, but not enacted upon, and instead the event worker just queued events.
 * (To verify, debug into StripedExecutor / StripedExecutor.Worker, and see events start to queue up).
 *
 * The same behaviour was found when we switched to Topic.
 * However, ReliableTopic seems more ... reliable.
 */
@Component
class StreamStateChangeEventListener(
   private val pipelineManager: PipelineManager,
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_BEAN_NAME)
   private val streamStateCache: IMap<String, StreamStatus>,
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_BEAN_NAME)
   val streamJobStateCache: IMap<StreamName, MutableList<StreamJobStateEvent>>,
   @Qualifier(StreamStateManagerHazelcastConfig.STREAM_STATUS_TOPIC_BEAN_NAME)
   val streamChangeEventTopic: ITopic<StreamStateChangeEvent>
) : StreamStateUpdatesPublisher {

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
      // Design choice: Using reliable topics for distributing steam state changes, rather than map listeners.
      // We originally used EntryListener here, but found it unreliable, and change messages got dropped
      streamChangeEventTopic
         .addMessageListener { message ->
            val messagePayload = message.messageObject
            if (streamStateCache.localKeySet().contains(messagePayload.name)) {
               logger.info { "Stream state has changed - acting. $messagePayload" }
               this.submitUpdatedStateToPipelineManager(messagePayload.newState, messagePayload.name.fqn())
            } else {
               logger.info { "Ignoring stream state change event, as the stream is managed by another node. $messagePayload" }
            }

         }
      emitCurrentState()
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


   private fun submitUpdatedStateToPipelineManager(
      value: StreamStatus,
      streamName: QualifiedName,
   ) {
      when (value.state) {
         StreamStatus.State.PAUSED -> {
            logger.info { "Stream $streamName entered state ${value.state}, so pausing on pipeline manager" }
            pipelineManager.suspendPipelineByName(streamName)
         }

         StreamStatus.State.RUNNING -> {
            logger.info { "Stream $streamName entered state ${value.state}, so submitting to pipeline manager" }
            pipelineManager.startPipelineByName(streamName)
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
