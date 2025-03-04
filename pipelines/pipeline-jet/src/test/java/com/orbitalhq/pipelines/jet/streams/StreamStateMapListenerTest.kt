package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.jet.Job
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamName
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import reactor.core.publisher.Flux
import reactor.kotlin.test.test
import java.time.Duration

class StreamStateMapListenerTest : DescribeSpec({
   describe("Listening for stream status updates") {
      val pipelineManager: PipelineManager = mock {
         on { jobStatusEvents } doReturn Flux.empty<Pair<Job, StreamJobStateEvent>>()
      }
      it("should publish stream statuses in construction time.") {
         val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()
         val streamStatusMap =
            hazelcast.getMap<String, StreamStatus>(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
         val jobStatusMap =
            hazelcast.getMap<StreamName, MutableList<StreamJobStateEvent>>(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_NAME)
         val streamChangeEventTopic = hazelcast.getReliableTopic<StreamStateChangeEvent>(StreamStateChangeEvent.topicName)
         streamStatusMap["stream-1"] = StreamStatus(streamName = "stream-1", state = StreamStatus.State.PAUSED)
         val streamStateChangeEventListener =
            StreamStateChangeEventListener(pipelineManager, streamStatusMap, jobStatusMap, streamChangeEventTopic)
         streamStateChangeEventListener
            .stateUpdates
            .test()
            .expectSubscription()
            .expectNextMatches { statesByStreamName ->
               statesByStreamName.entries.shouldHaveSize(1)
               val state = statesByStreamName["stream-1"]
                  .shouldNotBeNull()
               state.streamStatus.state.shouldBe(StreamStatus.State.PAUSED)
               true
            }
            .thenCancel()
            .verify()
         hazelcast.shutdown()
      }

      it("when a new streaming query is added its state should be reported correctly") {
         val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()
         val stateManager = StreamStateManager(pipelineManager = pipelineManager, hazelcastInstance = hazelcast)
         val streamStatusMap =
            hazelcast.getMap<String, StreamStatus>(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
         val streamChangeEventTopic = hazelcast.getReliableTopic<StreamStateChangeEvent>(StreamStateChangeEvent.topicName)
         val existingStream = StreamStatus(streamName = "stream-1", state = StreamStatus.State.PAUSED)
         streamStatusMap["stream-1"] = existingStream
         val jobStatusMap =
            hazelcast.getMap<StreamName, MutableList<StreamJobStateEvent>>(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_NAME)
         val streamStateChangeEventListener =
            StreamStateChangeEventListener(pipelineManager, streamStatusMap, jobStatusMap, streamChangeEventTopic)
//         val newStreamTobeAdded = StreamStatus(streamName = "stream-new", state = StreamStatus.State.PAUSED)
         streamStateChangeEventListener
            .stateUpdates
            .test()
            .expectSubscription()
            .expectNextMatches { statesByStreamName ->
               statesByStreamName.entries.shouldHaveSize(1)
               val state = statesByStreamName["stream-1"]
                  .shouldNotBeNull()
               state.streamStatus.state.shouldBe(StreamStatus.State.PAUSED)
               true
            }
            .then {
               //add a new stream.
               stateManager.getOrCreateStreamStatus("stream-new")
//               streamStatusMap["stream-new"] = newStreamTobeAdded
            }
            .expectNextMatches { statesByStreamName ->
               statesByStreamName.entries.shouldHaveSize(2)
               val state = statesByStreamName["stream-new"]
                  .shouldNotBeNull()
               state.streamStatus.state.shouldBe(StreamStatus.State.PAUSED)
               true
            }
            .thenCancel()
            .verify(Duration.ofSeconds(5))
         hazelcast.shutdown()

      }
   }
})
