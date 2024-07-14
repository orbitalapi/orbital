package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import reactor.kotlin.test.test

class StreamResultsServiceTest {

   @Test
   fun `publishes results to flux`() {
      val hazelcastInstance = TestHazelcastInstanceFactory().newHazelcastInstance()
      val service = StreamResultsService(hazelcastInstance)
      val streamName = "com.foo.TestStream"

      val topic = hazelcastInstance.getTopic<Any>(HazelcastTopicSinkSpec.topicNameForStream(streamName.fqn()))

      service.getResultStream(streamName)
         .test()
         .expectSubscription()
         .then {
            topic.publish("Hello, world")
         }
         .expectNext("Hello, world")
         .then {
            service.cacheSize.shouldBe(1L)
            topic.localTopicStats.receiveOperationCount.shouldBe(1)
         }
         .thenCancel()
         .verify()

      // Verify we remove the publisher from the cache
      // after the unsubscription
      service.cacheSize.shouldBe(0L)

      // Verify that we clean up the listener on the topic.
      (0..10).forEach { topic.publish("hello again $it") }
      topic.localTopicStats.receiveOperationCount.shouldBe(1)
   }
}
