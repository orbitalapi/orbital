package com.orbitalhq.connectors.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.KafkaConnectionRegistry
import com.orbitalhq.connectors.kafka.registry.toSenderOptions
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.format.FormatRegistry
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord

class KafkaStreamPublisher(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   fun write(
      connectionName: String,
      kafkaOperation: KafkaConnectorTaxi.Annotations.KafkaOperation,
      service: Service,
      operation: RemoteOperation,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      payload: TypedInstance,
      messageKey: TypedInstance?,
      schema: Schema
   ): Flow<TypedInstance> {
      val topic = kafkaOperation.topic
      val (connectionConfiguration, senderOptions) = buildSenderOptions(connectionName)

      val senderRecord = buildSenderRecord(payload, schema, topic, messageKey)
      val sender = KafkaSender.create(senderOptions)

      return sender.send(Mono.just(senderRecord))
         .doOnEach { _ -> meterRegistry.counter("orbital.connections.kafka.${connectionName}.topic.${topic}.messagesPublished").increment() }
         .doOnError { e->  logger.warn { "Failed to send message to Kafka connection ${connectionConfiguration.connectionName} on topic $topic: ${e::class.simpleName} - ${e.message}" } }
         .map { senderResult ->
            logger.info { "Message published to Kafka connection  ${connectionConfiguration.connectionName} on topic $topic with offset ${senderResult.recordMetadata()?.offset()}" }
            payload
         }
         .asFlow()
   }

   private fun buildSenderRecord(
      payload: TypedInstance,
      schema: Schema,
      topic: String,
      messageKey: TypedInstance?
   ): SenderRecord<Any?, Any?, Nothing?>? {
      val format = formatRegistry.forType(payload.type)
      val messageContent = if (format != null) {
         format.serializer.writeAsBytes(payload, schema)
      } else {
         objectMapper.writeValueAsBytes(payload.toRawObject())
      }
      return SenderRecord.create(topic, null, null, messageKey?.toRawObject(), messageContent, null)
   }

   private fun buildSenderOptions(connectionName: String): Pair<KafkaConnectionConfiguration, SenderOptions<Any, Any>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(connectionName)

      return connectionConfiguration to connectionConfiguration.toSenderOptions()
   }
}

