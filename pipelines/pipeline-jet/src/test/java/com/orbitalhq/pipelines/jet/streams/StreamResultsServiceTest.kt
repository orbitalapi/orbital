package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.test.TestHazelcastInstanceFactory
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import reactor.kotlin.test.test

class StreamResultsServiceTest {

   @Test
   fun `publishes results to flux`() {
      val hazelcastInstance = TestHazelcastInstanceFactory().newHazelcastInstance()
      val service = StreamResultsService(
         HazelcastStreamResultObserver(hazelcastInstance),
         mock { },
         mock { }
      )
      val streamName = "com.foo.TestStream"

      val topic = hazelcastInstance.getReliableTopic<Any>(HazelcastTopicSinkSpec.topicNameForStream(streamName.fqn()))

      service.getResultStream(streamName, principal = null)
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

      // This is failing for reliable topic, commenting out till we get a response from Hazelcast.
      // However, I can see that topic.removeMessageListener(subscriptionId) returns true in StreamResultsService
      topic.localTopicStats.receiveOperationCount.shouldBe(2)
   }
}
