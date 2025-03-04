package com.orbitalhq.pipelines.jet

import com.hazelcast.topic.ITopic
import com.hazelcast.topic.LocalTopicStats
import com.hazelcast.topic.Message
import com.hazelcast.topic.MessageListener
import com.nhaarman.mockito_kotlin.mock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import reactor.core.publisher.Sinks
import java.util.*
import java.util.concurrent.CompletionStage

/**
 * Simple implementation of Hazelcast's ITopic for unit tests that aren't using Hazelcast.
 *
 */
class TestTopic<T>(val topicName: String = "TestTopic") : ITopic<T> {
   private val sink = Sinks.many().multicast().directAllOrNothing<Message<T>>()
   override fun getPartitionKey(): String {
      TODO("Not yet implemented")
   }

   override fun getName(): String {
      TODO("Not yet implemented")
   }

   override fun getServiceName(): String {
      TODO("Not yet implemented")
   }

   override fun destroy() {
      TODO("Not yet implemented")
   }

   override fun removeMessageListener(registrationId: UUID): Boolean {
      TODO("Not yet implemented")
   }

   override fun getLocalTopicStats(): LocalTopicStats {
      TODO("Not yet implemented")
   }

   override fun publishAllAsync(messages: MutableCollection<out T>): CompletionStage<Void> {
      TODO("Not yet implemented")
   }

   override fun publishAll(messages: MutableCollection<out T>) {
      TODO("Not yet implemented")
   }

   override fun addMessageListener(listener: MessageListener<T>): UUID {
      sink.asFlux()
         .subscribe { message ->
            listener.onMessage(message)
         }
      return UUID.randomUUID()
   }

   override fun publishAsync(message: T & Any): CompletionStage<Void> {
      TODO("Not yet implemented")
   }

   override fun publish(message: T & Any) {
      sink.tryEmitNext(
         Message(topicName,message, java.time.Instant.now().toEpochMilli(), mock {  })
      )
   }
}
