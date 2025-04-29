package com.orbitalhq.connectors.kafka

import com.google.common.base.Throwables
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.toAdminProps
import com.orbitalhq.connectors.kafka.registry.toConsumerProps
import com.orbitalhq.metrics.GaugeRegistry
import com.orbitalhq.metrics.MetricTags
import com.orbitalhq.utils.orElse
import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.ConsumerGroupDescription
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.ReceiverOptions
import java.io.Serializable
import java.time.Duration


private data class MonitoredKafkaConsumerTopic(
   val request: KafkaConsumerRequest,
   val connectionConfiguration: KafkaConnectionConfiguration,
   val receiverOptions: ReceiverOptions<Any, ByteArray>,
) {
   override fun toString(): String {
      return "Connection: ${request.connectionName}, topic: ${request.topicName}, consumer: ${
         request.streamSourceId?.orElse(
            "Not set"
         )
      }"
   }
}

/**
 * Builds a flow emitting stats on consumer group usage.
 * Built to help users who are struggling to detect why messages aren't being received
 * by orbital - we need to improve visibility of the consumer behaviour.
 */
class KafkaConsumerStatsFlowBuilder(
   private val gaugeRegistry: GaugeRegistry,
   pollFrequency: Duration = Duration.ofSeconds(15),
   private val operationTimeout: Duration = Duration.ofSeconds(10)
) {

   private val monitoringScheduler = Schedulers.newBoundedElastic(
      5, // Number of threads
      100, // Queue size
      "kafka-monitoring"
   )

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   // We need to retain admin clients and consumers.
   // Otherwise, each time we publish stats, it creates a new connection, which spams the logs.
   private val adminClient = ConcurrentHashMap<KafkaConnectionConfiguration, AdminClient>()
   private val consumer = ConcurrentHashMap<KafkaConnectionConfiguration, KafkaConsumer<Any, Any>>()

   private val monitoredTopics = ConcurrentHashMap<KafkaConsumerRequest, MonitoredKafkaConsumerTopic>()

   init {
      monitoringScheduler.schedule {
         Flux.interval(pollFrequency, monitoringScheduler)
            .publishOn(monitoringScheduler)
            .onBackpressureBuffer() // If emitConsumerStat takes longer than pollFrequency, don't schedule new one when emitConsumerStats is in progress.
            .subscribeOn(monitoringScheduler)
            .subscribe {
               try {
                  logger.debug { "Starting to update Kafka monitoring stats" }
                  val monos = emitConsumerStats()
                  Flux.concat(monos)
                     .subscribeOn(monitoringScheduler)
                     .doOnComplete {
                        logger.debug { "Finished updating Kafka monitoring stats" }
                     }
                     .subscribe()
               } catch (e: Exception) {
                  logger.warn(e) { "Exception thrown while monitoring kafka stats" }
               }
            }
      }

   }

   // Note: Refactored this to put the interactions with AdminClient / KafkaConsumer
   // on a single thread.
   // We want a long-lived adminClient and consumer, but KafkaConsumer is not safe for multithreaded
   // access (it throws an exception saying as much),
   // so we work by storing a list of monitored topics, and emitting stats from a single thread.
   private fun emitConsumerStats(): List<Mono<out MutableMap<out Serializable, out Any>>> {
      return monitoredTopics.flatMapIndexed { index, monitoringConfig ->

         logger.debug { "Capturing Kafka consumer stats for $monitoringConfig (${index + 1} / ${monitoredTopics.size})" }

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
         val consumerGroupMetricsMono = emitConsumerGroupMetrics(adminClient, groupId, connectionConfiguration, request, monitoringConfig)


         // Get current consumer group offsets asynchronously
         val topicOffsetMono = emitConsumerGroupTopicOffsets(
            adminClient,
            groupId,
            partitionsInfo,
            endOffsets,
            connectionConfiguration,
            request,
            monitoringConfig
         )
         listOf(consumerGroupMetricsMono,topicOffsetMono)

      }
   }

   private fun emitConsumerGroupTopicOffsets(
      adminClient: AdminClient,
      groupId: String,
      partitionsInfo: MutableList<TopicPartition>,
      endOffsets: MutableMap<TopicPartition, Long>,
      connectionConfiguration: KafkaConnectionConfiguration,
      request: KafkaConsumerRequest,
      monitoringConfig: MonitoredKafkaConsumerTopic?
   ): Mono<MutableMap<TopicPartition, OffsetAndMetadata>> {
      return Mono.fromFuture {
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
                        MetricTags.Topic.of(partition.topic()),
                        MetricTags.KafkaPartition.of(partition.partition()),
                        MetricTags.KafkaGroupId.of(groupId)
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
                  }
               } else {
                  val rootCause = Throwables.getRootCause(throwable)
                  logger.warn { "Failed to monitor consumer offsets for Kafka connection $monitoringConfig - ${rootCause.message ?: "A ${rootCause::class.simpleName} exception was thrown"}" }
               }
            }
            .toCompletionStage().toCompletableFuture()
      }
         .timeout(operationTimeout)
         .doOnError { throwable ->
            val rootCause = Throwables.getRootCause(throwable)
            logger.warn { "Failed to monitor consumer offsets for Kafka connection $monitoringConfig - ${rootCause.message ?: "A ${rootCause::class.simpleName} exception was thrown"}" }
         }
   }

   private fun emitConsumerGroupMetrics(
      adminClient: AdminClient,
      groupId: String,
      connectionConfiguration: KafkaConnectionConfiguration,
      request: KafkaConsumerRequest,
      monitoringConfig: MonitoredKafkaConsumerTopic?
   ): Mono<MutableMap<String, ConsumerGroupDescription>> {
      return Mono.fromFuture {
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
                        MetricTags.KafkaGroupId.of(groupId)
                     )
                  ).set(members.size)


               } else {
                  val rootCause = Throwables.getRootCause(throwable)
                  logger.warn { "Failed to monitor consumer group info  for Kafka connection $monitoringConfig - ${rootCause.message ?: "A ${rootCause::class.simpleName} exception was thrown"}" }
               }
            }.toCompletionStage().toCompletableFuture()
      }.timeout(operationTimeout)
         .doOnError { throwable ->
            val rootCause = Throwables.getRootCause(throwable)
            logger.warn { "Failed to monitor consumer group info  for Kafka connection $monitoringConfig - ${rootCause.message ?: "A ${rootCause::class.simpleName} exception was thrown"}" }
         }

   }

   /**
    * Registers a Kafka connection / consumer for emitting monitoring stats.
    * Stats mesaured include:
    * - Number of consumers in the consumer group
    * - Current offset of our consumer group
    * - Head offset of the topic (for lag calculation)
    */
   fun startMonitoring(
      request: KafkaConsumerRequest,
      connectionConfiguration: KafkaConnectionConfiguration,
      receiverOptions: ReceiverOptions<Any, ByteArray>,
   ) {
      monitoredTopics.computeIfAbsent(request) {
         logger.info { "starting monitoring of ${request.connectionName} / ${request.topicName}" }
         MonitoredKafkaConsumerTopic(request, connectionConfiguration, receiverOptions)
      }


   }

   fun stopMonitoring(consumerRequest: KafkaConsumerRequest) {
      logger.info { "stopping monitoring of ${consumerRequest.connectionName} / ${consumerRequest.topicName}" }
      monitoredTopics.remove(consumerRequest)
   }


   private fun Sinks.EmitResult.logIfFailed() {
      if (this.isFailure) {
         logger.info { "Failed to emit event: $this" }
      }
   }
}

data class KafkaConsumerGroupInfoMessage(
   val message: String,
   val request: KafkaConsumerRequest,
)

