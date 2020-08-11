package io.vyne.pipelines.runner.transport.kafka

import reactor.core.publisher.Flux
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord

/**
 * This interface exists to help make testing of Kafka streams easier.
 * It allows for injecting a mocked feed (which produced Flux<ReceiverRecord<String,V>>,
 * removing the need for a kafka instance running.
 * I wrote this because of issues using EmbeddedKafka and version conflicts in the
 * various Kafka/Scala versions in our dependency tree
 */
interface KafkaConnectionFactory<V> {
   fun createReceiver(options: ReceiverOptions<String, V>): Pair<KafkaReceiver<String, V>, Flux<ReceiverRecord<String, V>>>
}

class DefaultKafkaConnectionFactory<V> : KafkaConnectionFactory<V> {
   override fun createReceiver(options: ReceiverOptions<String, V>): Pair<KafkaReceiver<String, V>, Flux<ReceiverRecord<String, V>>> {
      val receiver = KafkaReceiver.create(options)
      val feed = receiver.receive()
      return receiver to feed
   }
}
