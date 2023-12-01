package com.orbitalhq.spring.projection

import com.hazelcast.cluster.Member
import com.hazelcast.cluster.MemberSelector
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IExecutorService
import com.orbitalhq.Vyne
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.facts.FactBag
import com.orbitalhq.models.serde.SerializableTypedInstance
import com.orbitalhq.models.serde.toSerializable
import com.orbitalhq.query.MetricTags
import com.orbitalhq.query.Projection
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.TypedInstanceWithMetadata
import com.orbitalhq.query.projection.ProjectionProvider
import com.orbitalhq.query.withProcessingMetadata
import com.spikhalskiy.futurity.Futurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.time.LocalDateTime
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class HazelcastProjectionProvider(val taskSize: Int, private val nonLocalDistributionClusterSize: Int = 10) :
   ProjectionProvider {

   val hazelcastScheduler: Scheduler = Schedulers.parallel()

   override fun project(
      results: Flow<TypedInstance>,
      projection: Projection,
      context: QueryContext,
      globalFacts: FactBag,
      metricsTags: MetricTags
   ): Flow<TypedInstanceWithMetadata> {

      val instance: HazelcastInstance = Hazelcast.getAllHazelcastInstances().first()
      val executorService: IExecutorService = instance.getExecutorService("projectionExecutorService")

      val vyne = ApplicationContextProvider.context()!!.getBean("vyneFactory") as Vyne

      val selector = if (instance.cluster.members.size >= nonLocalDistributionClusterSize) {
         RemoteBiasedMemberSelector()
      } else {
         MemberSelector { true }
      }

      val projectedResults = results
         .asFlux()
         .filter { !context.cancelRequested }
         .buffer(taskSize) //Take buffers of provided buffer size - this determines the size of distributed work packet
         .index()
         .parallel()
         .runOn(hazelcastScheduler)
         .map {
            Instant.now() to toDistributableTask(
               context,
               projection,
               it.t2,
               it.t1
            )
         } //Serialize to task
         .map { (startTime, task) ->
            startTime to (Futurity.shift(executorService.submit(task.first, selector)).toMono() to task.second)
         } //Distribute on hazelcast
         .map { (startTime, taskResults) ->
            deserialiseTaskResults(context, vyne, taskResults.first, taskResults.second)
               .map { it.withProcessingMetadata(asOf = startTime) }
         }

      return projectedResults.asFlow().flatMapMerge { it }

   }

   private fun toDistributableTask(
      context: QueryContext,
      projection: Projection,
      input: List<TypedInstance>,
      segment: Long
   ): Pair<HazelcastProjectingTask, Long> {

      logger.info { "Distributing segment $segment for query ${context.queryId} at ${LocalDateTime.now()}" }
      val serializedTypedInstancesAsByteList = input.map { it.toSerializable().toBytes() }
      val serializedExcludedServices = Cbor.encodeToByteArray(context.excludedServices)
      val actualProjectedType = projection.type.collectionType ?: projection.type
      val qualifiedName = actualProjectedType.qualifiedName
      return HazelcastProjectingTask(
         context.queryId,
         serializedTypedInstancesAsByteList,
         serializedExcludedServices,
         qualifiedName,
         segment
      ) to segment
   }

   private fun deserialiseTaskResults(
      context: QueryContext,
      vyne: Vyne,
      resultsFuture: Mono<ByteArray>,
      segment: Long
   ): Flow<TypedInstance> {

      val deserialised =
         resultsFuture
            .map {
               val decoded = Cbor.decodeFromByteArray<List<ByteArray>>(it)
                  .map {
                     SerializableTypedInstance.fromBytes(it).toTypedInstance(vyne.schema)
                  }
                  .toFlux()
               logger.info { "Received and deserialised segment $segment for query ${context.queryId} at ${Instant.now()}" }
               decoded
            }
      return deserialised.flatMapMany { it }.asFlow()

   }

}

/**
 * Hazelcast member selector class allow inclusion of work on the local instance only 1/2 of the time
 * averaged the local node will only perform half of the work of other nodes - allowing the localnode - where
 * the query is running CPU capacity for orchestration and deserialising of work remote work results
 */
class RemoteBiasedMemberSelector : MemberSelector {
   override fun select(member: Member): Boolean {
      return if (Random.nextBoolean()) {
         !member.localMember() && member.attributes[VyneHazelcastMemberTags.VYNE_TAG.tag] == VyneHazelcastMemberTags.QUERY_SERVICE_TAG.tag
      } else {
         member.attributes[VyneHazelcastMemberTags.VYNE_TAG.tag] == VyneHazelcastMemberTags.QUERY_SERVICE_TAG.tag
      }
   }
}

enum class VyneHazelcastMemberTags(val tag: String) {
   VYNE_TAG("vyneTag"),
   QUERY_SERVICE_TAG("vyne-query-service")
}

