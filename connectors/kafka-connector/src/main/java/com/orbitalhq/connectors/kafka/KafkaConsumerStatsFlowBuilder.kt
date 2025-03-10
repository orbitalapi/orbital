package com.orbitalhq.connectors.kafka

import com.google.common.base.Throwables
import com.orbitalhq.connectors.StreamErrorMessage
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.toAdminProps
import com.orbitalhq.connectors.kafka.registry.toConsumerProps
import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.metrics.MetricTags
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


private data class MonitoredKafkaConsumerTopic(
   val request: KafkaConsumerRequest,
   val connectionConfiguration: KafkaConnectionConfiguration,
   val receiverOptions: ReceiverOptions<Any, ByteArray>,
)

/**
 * Builds a flow emitting stats on consumer group usage.
 * Built to help users who are struggling to detect why messages aren't being received
 * by orbital - we need to improve visibility of the consumer behaviour.
 */
class KafkaConsumerStatsFlowBuilder(
   private val gaugeRegistry: GaugeRegistry,
   private val pollFrequency: Duration = Duration.ofSeconds(15)
) {


   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // We need to retain admin clients and consumers.
   // Otherwise, each time we publish stats, it creates a new connection, which spams the logs.
   private val adminClient = ConcurrentHashMap<KafkaConnectionConfiguration, AdminClient>()
   private val consumer = ConcurrentHashMap<KafkaConnectionConfiguration, KafkaConsumer<Any, Any>>()

   private val monitoredTopics = mutableSetOf<MonitoredKafkaConsumerTopic>()

   private val monitoringStatusMessageSink = Sinks.many().multicast().directBestEffort<KafkaConsumerGroupInfoMessage>()
   private val monitoringStatusMessageFlux = monitoringStatusMessageSink.asFlux()


   init {
      Flux.interval(pollFrequency)
         .subscribe { emitConsumerStats() }
   }

   // Note: Refactored this to put the interactions with AdminClient / KafkaConsumer
   // on a single thread.
   // We want a long-lived adminClient and consumer, but KafkaConsumer is not safe for multi-threaded
   // access (it throws an exception saying as much),
   // so we work by storing a list of monitoed topics, and emitting stats from a single thread.
   private fun emitConsumerStats() {
      monitoredTopics.forEach { monitoringConfig ->
         val messages = mutableListOf<KafkaConsumerGroupInfoMessage>()
         val connectionConfiguration = monitoringConfig.connectionConfiguration
         val receiverOptions = monitoringConfig.receiverOptions
         val request = monitoringConfig.request

         val adminProps = connectionConfiguration.toAdminProps()
         val consumerProps = connectionConfiguration.toConsumerProps(offset = "latest")
         val groupId = receiverOptions.consumerProperties()["group.id"] as String
         val topics = receiverOptions.subscriptionTopics() ?: emptyList()
         val adminClient = try {
            adminClient.getOrPut(connectionConfiguration) { AdminClient.create(adminProps) }
         } catch (e: Exception) {
            val rootCauseMessage = Throwables.getRootCause(e).message
            throw IllegalArgumentException("Failed to construct admin client for connection ${request.connectionName}: ${e.message} - $rootCauseMessage")
         }
         val consumer = consumer.getOrPut(connectionConfiguration) { KafkaConsumer(consumerProps) }
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
                  gaugeRegistry.int(
                     "orbital.connections.kafka.members",
                     listOf(
                        MetricTags.ConnectionName.of(connectionConfiguration.connectionName),
                        MetricTags.Topic.of(request.topicName),
                        MetricTags.KafkaGroupId.of(groupId)
                     )
                  ).set(members.size)
                  messages.add(
                     KafkaConsumerGroupInfoMessage(
                        "Consumer Group: $groupId has ${members.size} member(s): ${members.joinToString { it.consumerId() }}",
                        request
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
                        val tags = listOf(
                           MetricTags.ConnectionName.of(connectionConfiguration.connectionName),
                           MetricTags.Topic.of(request.topicName),
                           MetricTags.KafkaPartition.of(partition.partition())
                        )
                        gaugeRegistry.long(
                           "orbital.connections.kafka.lag",
                           tags
                        ).set(lag)
                        gaugeRegistry.long(
                           "orbital.connections.kafka.end",
                           tags
                        ).set(endOffset)
                        gaugeRegistry.long(
                           "orbital.connections.kafka.offset",
                           tags
                        ).set(currentOffset)
                        messages.add(KafkaConsumerGroupInfoMessage("Partition ${partition.partition()} current offset: $currentOffset, end: $endOffset, lag: $lag", request))
                     }
                  }
               }.get()
         } catch (e: Exception) {
            logger.warn { "Exception thrown trying to fetch offsets: ${e.message}" }
         }
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
      monitoredTopics.add(MonitoredKafkaConsumerTopic(request, connectionConfiguration, receiverOptions))
      return monitoringStatusMessageFlux
         .filter { message -> message.request == request }
         .asFlow()
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
   val message: String,
   val request: KafkaConsumerRequest,
)

