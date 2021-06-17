package io.vyne.cask.observers.kafka

import io.vyne.cask.observers.ObservedChange
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFutureCallback

private val logger = KotlinLogging.logger {}
class KafkaMessageWriter(private val template: KafkaTemplate<String, ObservedChange>) {
   fun send(id: String, value: ObservedChange, topic: String) {
      template
         .send(topic, id, value)
         .addCallback(object : ListenableFutureCallback<SendResult<String, ObservedChange>> {
            override fun onSuccess(sucessResult: SendResult<String, ObservedChange>?) {
               logger.trace { "successfully sent $id to $topic" }
            }
            override fun onFailure(error: Throwable) {
               logger.error(error) { "Error in sending $id to $topic" }
            }
         })
   }
}
