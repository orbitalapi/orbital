package com.orbitalhq.pipelines.jet.streams

import com.hazelcast.client.test.TestHazelcastFactory
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.pipelines.jet.api.transport.hazelcast.HazelcastTopicSinkSpec
import com.orbitalhq.schemas.fqn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
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
         }
         .thenCancel()
         .verify()

      // Verify we remove the publisher from the cache
      // after the unsubscription
      service.cacheSize.shouldBe(0L)
   }
}
