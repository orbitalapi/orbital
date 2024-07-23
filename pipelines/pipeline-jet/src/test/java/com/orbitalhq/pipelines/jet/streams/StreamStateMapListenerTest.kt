package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.pipelines.jet.api.streams.StreamStatus
import com.orbitalhq.pipelines.jet.pipelines.PipelineManager
import io.kotest.core.spec.style.DescribeSpec
import reactor.kotlin.test.test

class StreamStateMapListenerTest: DescribeSpec({
    describe("Listening for stream status updates") {
        it("should publish stream statuses in construction time.") {
           val pipelineManager: PipelineManager = mock { }
            val hazelcast = TestHazelcastInstanceFactory(1).newHazelcastInstance()
            val streamStatusMap = hazelcast.getMap<String, StreamStatus>(StreamStateManagerHazelcastConfig.STREAM_STATUS_CACHE_NAME)
            streamStatusMap["stream-1"] = StreamStatus(streamName = "stream-1", state = StreamStatus.State.PAUSED)
            val streamStateMapListener = StreamStateMapListener(pipelineManager, streamStatusMap)
            streamStateMapListener
                .stateUpdates
                .test()
                .expectSubscription()
                .expectNextMatches { it.size == 1 && it.first().streamName == "stream-1" && it.first().state == StreamStatus.State.PAUSED }
                .thenCancel()
                .verify()
            hazelcast.shutdown()
        }
    }
})