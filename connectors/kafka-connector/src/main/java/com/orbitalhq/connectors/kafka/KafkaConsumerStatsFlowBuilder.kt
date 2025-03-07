package com.orbitalhq.connectors.kafka

import com.google.common.base.Throwables
import com.orbitalhq.connectors.StreamErrorMessage
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.toAdminProps
import com.orbitalhq.connectors.kafka.registry.toConsumerProps
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.kafka.receiver.ReceiverOptions
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Builds a flow emitting stats on consumer group usage.
 * Built to help users who are struggling to detect why messages aren't being received
 * by orbital - we need to improve visibility of the consumer behaviour.
 */
class KafkaConsumerStatsFlowBuilder(
   private val meterRegistry: MeterRegistry,
   private val pollFrequency: Duration = Duration.ofSeconds(15)
) {


   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // We need to retain admin clients and consumers.
   // Otherwise, each time we publish stats, it creates a new connection, which spams the logs.
   private val adminClients = ConcurrentHashMap<KafkaConnectionConfiguration, AdminClient>()
   private val consumers = ConcurrentHashMap<KafkaConnectionConfiguration, KafkaConsumer<Any, Any>>()

   private val intGauges = mutableMapOf<String, AtomicInteger>()
   private val longGauges = mutableMapOf<String, AtomicLong>()
   private fun intGauge(name: String): AtomicInteger {
      return intGauges.getOrPut(name) {
         meterRegistry.gauge(name, AtomicInteger(0))!!
      }
   }

   private fun longGauge(name: String): AtomicLong {
      return longGauges.getOrPut(name) {
         meterRegistry.gauge(name, AtomicLong(0))!!
      }
   }

   /**
    * Builds a flow that emits consumer group statistics without blocking.
    * The flow emits messages as each piece of data becomes available and then completes.
    * Messages include:
    * - Number of consumers in the consumer group
    * - Current offset of our consumer group
    * - Head offset of the topic (for lag calculation)
    */
   fun buildConsumerStatsFlow(
      request: KafkaConsumerRequest,
      connectionConfiguration: KafkaConnectionConfiguration,
      receiverOptions: ReceiverOptions<Any, ByteArray>,
   ): Flow<KafkaConsumerGroupInfoMessage> {
      val adminProps = connectionConfiguration.toAdminProps()
      val consumerProps = connectionConfiguration.toConsumerProps(offset = "latest")
      val groupId = receiverOptions.consumerProperties()["group.id"] as String
      val topics = receiverOptions.subscriptionTopics() ?: emptyList()

      return Flux.just(0L)
         .concatWith(Flux.interval(pollFrequency))
         .flatMapIterable {
            val messages = mutableListOf<KafkaConsumerGroupInfoMessage>()
            val adminClient = try {
               adminClients.getOrPut(connectionConfiguration) { AdminClient.create(adminProps) }
            } catch (e: Exception) {
               val rootCauseMessage = Throwables.getRootCause(e).message
               throw IllegalArgumentException("Failed to construct admin client for connection ${request.connectionName}: ${e.message} - $rootCauseMessage")
            }
            val consumer = consumers.getOrPut(connectionConfiguration) { KafkaConsumer(consumerProps) }
            // Get the topic-partitions to check offsets
            val partitionsInfo = mutableListOf<TopicPartition>()
            for (topic in topics) {
               val partitions = consumer.partitionsFor(topic)
               for (partitionInfo in partitions) {
                  partitionsInfo.add(TopicPartition(partitionInfo.topic(), partitionInfo.partition()))
               }
            }

            // Get end offsets for all topic-partitions (this is synchronous but fast)
            val endOffsets = consumer.endOffsets(partitionsInfo)

            // Get consumer groups
            adminClient.describeConsumerGroups(listOf(groupId))
               .all()
               .whenComplete { groupDescriptions, throwable ->
                  if (throwable == null) {
                     val groupInfo = groupDescriptions[groupId]
                     val members = groupInfo?.members() ?: emptyList()
                     intGauge(
                        "orbital.connections.kafka.${connectionConfiguration.connectionName}.topic.${request.topicName}.consumerGroup.${groupId}.members",
                     ).set(members.size)
                     messages.add(
                        KafkaConsumerGroupInfoMessage(
                           "Consumer Group: $groupId has ${members.size} member(s): ${members.joinToString { it.consumerId() }}"
                        )
                     )
                  }
               }
               .get()


            // Get current consumer group offsets asynchronously
            try {
               adminClient.listConsumerGroupOffsets(groupId)
                  .partitionsToOffsetAndMetadata()
                  .whenComplete { currentOffsets, throwable ->
                     if (throwable == null) {
                        partitionsInfo.map { partition ->
                           val currentOffset = currentOffsets[partition]?.offset() ?: 0L
                           val endOffset = endOffsets[partition] ?: 0L
                           val lag = endOffset - currentOffset
                           longGauge(
                              "orbital.connections.kafka.${connectionConfiguration.connectionName}.topic.${request.topicName}.partition.${partition.partition()}.lag",
                           ).set(lag)
                           longGauge(
                              "orbital.connections.kafka.${connectionConfiguration.connectionName}.topic.${request.topicName}.partition.${partition.partition()}.end",
                           ).set(endOffset)
                           longGauge(
                              "orbital.connections.kafka.${connectionConfiguration.connectionName}.topic.${request.topicName}.partition.${partition.partition()}.offset",
                           ).set(currentOffset)
                           messages.add(KafkaConsumerGroupInfoMessage("Partition ${partition.partition()} current offset: $currentOffset, end: $endOffset, lag: $lag"))
                        }
                     }
                  }.get()
            } catch (e: Exception) {
               logger.warn { "Exception thrown trying to fetch offsets: ${e.message}" }
            }
            messages
         }.asFlow()
   }

   /**
    * Currently all streams emit either a TypedInstance or an error.
    * We need to chhange that to allow non-error messages (such as these).
    * However, theres' other refactoring happening in that area right now.
    *
    * As a workaround, modify our messages to look like errors.
    */
   fun buildAndWrapAsErrorMessages(
      request: KafkaConsumerRequest,
      connectionConfiguration: KafkaConnectionConfiguration,
      receiverOptions: ReceiverOptions<Any, ByteArray>,
   ): Flow<StreamErrorMessage> {
      return buildConsumerStatsFlow(request, connectionConfiguration, receiverOptions)
         .map {
            StreamErrorMessage(
               Instant.now(),
               RuntimeException("This is not a real exception"),
               it.message,
               "",
               ""
            )
         }
   }

   fun Sinks.EmitResult.logIfFailed() {
      if (this.isFailure) {
         logger.info { "Failed to emit event: $this" }
      }
   }
}

data class KafkaConsumerGroupInfoMessage(
   val message: String
)

