package com.orbitalhq.connectors.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
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
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.util.concurrent.TimeUnit

class KafkaStreamPublisher(
   private val connectionRegistry: KafkaConnectionRegistry,
   private val objectMapper: ObjectMapper = Jackson.defaultObjectMapper,
   private val formatRegistry: FormatRegistry,
   private val meterRegistry: MeterRegistry
) {

   private val cache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .removalListener<String, Pair<Disposable, Sinks.Many<SenderRecord<Any?, Any?, Nothing?>>>> {
         logger.warn { "Disposing the KafkaStreamPublisher for ${it.key}" }
         it.value?.first?.dispose()
         it.value?.second?.tryEmitComplete()
      }
      .build<String, Pair<Disposable, Sinks.Many<SenderRecord<Any?, Any?, Nothing?>>>>()
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
      val sender = cache.get(connectionName) {
         val (connectionConfiguration, senderOptions) = buildSenderOptions(connectionName)
         val sink = Sinks.many()
            .unicast()
            .onBackpressureBuffer<SenderRecord<Any?, Any?, Nothing?>>()

         val kafkaSender = KafkaSender.create(senderOptions)

         /*********************************************************************************************************************
          * Calling kafkaSender.send(Mono<SenderRecord>) for each 'TypedInstance' is much slower than calling kafkaSender.send(Publisher) once
          * and injecting TypedInstances through the Publisher.
          *********************************************************************************************************************/
         val disposable =  kafkaSender
            .send(sink.asFlux())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnEach { _ -> meterRegistry.counter("orbital.connections.kafka.${connectionName}.topic.${topic}.messagesPublished").increment() }
            .subscribe()

         disposable to sink
      }
      val senderRecord = buildSenderRecord(payload, schema, topic, messageKey)
      sender.second.emitNext(senderRecord, RetryFailOnSerializeEmitHandler)
      return flow { emit(payload) }
   }

   private fun buildSenderRecord(
      payload: TypedInstance,
      schema: Schema,
      topic: String,
      messageKey: TypedInstance?
   ): SenderRecord<Any?, Any?, Nothing?> {
      val (metadata, format) = formatRegistry.forType(payload.type)
      val messageContent = if (format != null) {
         format.serializer.writeAsBytes(payload, metadata!!, schema, -1)
      } else {
         val rawObject = payload.toRawObject()
         objectMapper.writeValueAsBytes(rawObject)
      }
      return SenderRecord.create(topic, null, null, messageKey?.toRawObject(), messageContent, null)
   }

   private fun buildSenderOptions(connectionName: String): Pair<KafkaConnectionConfiguration, SenderOptions<Any, Any>> {
      val connectionConfiguration =
         connectionRegistry.getConnection(connectionName)

      return connectionConfiguration to connectionConfiguration.toSenderOptions()
   }
}

