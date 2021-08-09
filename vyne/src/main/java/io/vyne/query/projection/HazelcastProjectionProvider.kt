package io.vyne.query.projection

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IExecutorService
import com.hazelcast.core.Member
import com.hazelcast.core.MemberSelector
import com.spikhalskiy.futurity.Futurity
import io.vyne.Vyne
import io.vyne.models.SerializableTypedInstance
import io.vyne.models.TypedInstance
import io.vyne.models.toSerializable
import io.vyne.query.QueryContext
import io.vyne.query.SerializableVyneQueryStatistics
import io.vyne.query.VyneQueryStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
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
import java.time.LocalDateTime
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class HazelcastProjectionProvider(val taskSize: Int, private val nonLocalDistributionClusterSize: Int = 10) : ProjectionProvider {

    val hazelcastScheduler: Scheduler = Schedulers.parallel()

    override fun project(results: Flow<TypedInstance>, context:QueryContext):Flow<Pair<TypedInstance, VyneQueryStatistics>> {

        val instance:HazelcastInstance = Hazelcast.getAllHazelcastInstances().first()
        val executorService:IExecutorService = instance.getExecutorService("projectionExecutorService")

        val vyne = ApplicationContextProvider!!.context()!!.getBean("vyneFactory") as Vyne

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
            .runOn( hazelcastScheduler )
            .map { toDistributableTask(context, it.t2, it.t1) } //Serialize to task
            .map { Futurity.shift(executorService.submit(it.first, selector)).toMono() to it.second } //Distribute on hazelcast
            .map { deserialiseTaskResults(context, vyne, it.first, it.second) }

        return projectedResults.asFlow().flatMapMerge { it }

    }

    private fun toDistributableTask(context: QueryContext, input: List<TypedInstance>, segment:Long):Pair<HazelcastProjectingTask, Long> {

        logger.info { "Distributing segment $segment for query ${context.queryId} at ${LocalDateTime.now()}" }
        val serializedTypedInstances = input.map { Cbor.encodeToByteArray( it.toSerializable() ) }
        val serializedExcludedServices = Cbor.encodeToByteArray(context.excludedServices)
        val actualProjectedType = context.projectResultsTo?.collectionType ?: context.projectResultsTo
        val qualifiedName = actualProjectedType!!.qualifiedName
        return HazelcastProjectingTask(context.queryId, serializedTypedInstances, serializedExcludedServices, qualifiedName, segment) to segment
    }

    private fun deserialiseTaskResults(context: QueryContext, vyne:Vyne, resultsFuture: Mono<ByteArray>, segment:Long):Flow<Pair<TypedInstance, VyneQueryStatistics>> {

        val deserialised =
            resultsFuture
                .map {
                    val decoded = Cbor.decodeFromByteArray<List<Pair<SerializableTypedInstance, SerializableVyneQueryStatistics>>>(it)
                        .map{
                            it.first.toTypedInstance(vyne.schema) to VyneQueryStatistics.from(it.second)
                        }
                        .toFlux()
                    logger.info { "Received and deserialised segment $segment for query ${context.queryId} at ${LocalDateTime.now()}" }
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

        return if ( Random.nextBoolean() ) {
            !member.localMember()
        } else {
            true
        }
    }
}
