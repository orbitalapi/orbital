package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.pipelines.jet.api.streams.StreamJobStateEvent
import com.orbitalhq.pipelines.jet.api.streams.StreamName
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import reactor.kotlin.test.test

class StreamStateMapListenerTest: DescribeSpec({
    describe("Listening for stream status updates") {
        val pipelineManager: PipelineManager = mock { }
        it("should publish stream statuses in construction time.") {
            val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()
            val streamStatusMap = hazelcast.getMap<String, StreamStatus>(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
            val jobStatusMap = hazelcast.getMap<StreamName, MutableList<StreamJobStateEvent>>(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_NAME)
            streamStatusMap["stream-1"] = StreamStatus(streamName = "stream-1", state = StreamStatus.State.PAUSED)
            val streamStateMapListener = StreamStateMapListener(pipelineManager, streamStatusMap, jobStatusMap)
            streamStateMapListener
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
            val streamStatusMap = hazelcast.getMap<String, StreamStatus>(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
            val existingStream = StreamStatus(streamName = "stream-1", state = StreamStatus.State.PAUSED)
            streamStatusMap["stream-1"] = existingStream
           val jobStatusMap = hazelcast.getMap<StreamName, MutableList<StreamJobStateEvent>>(StreamStateManagerHazelcastConfig.STREAM_JOB_STATUS_CACHE_NAME)
           val streamStateMapListener = StreamStateMapListener(pipelineManager, streamStatusMap, jobStatusMap)
            val newStreamTobeAdded = StreamStatus(streamName = "stream-new", state = StreamStatus.State.PAUSED)
            streamStateMapListener
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
                    streamStatusMap["stream-new"] = newStreamTobeAdded
                }
               .expectNextMatches { statesByStreamName ->
                  statesByStreamName.entries.shouldHaveSize(2)
                  val state = statesByStreamName["stream-new"]
                     .shouldNotBeNull()
                  state.streamStatus.state.shouldBe(StreamStatus.State.PAUSED)
                  true
                }
                .thenCancel()
                .verify()
            hazelcast.shutdown()

        }
    }
})
