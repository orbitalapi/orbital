package io.vyne.spring.projection

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.HazelcastInstanceAware
import io.vyne.Vyne
import io.vyne.models.CopyOnWriteFactBag
import io.vyne.models.serde.SerializableTypedInstance
import io.vyne.models.serde.toSerializable
import io.vyne.query.QueryContext
import io.vyne.query.QueryProfiler
import io.vyne.query.SearchGraphExclusion
import io.vyne.query.SerializableVyneQueryStatistics
import io.vyne.schemas.QualifiedName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import mu.KotlinLogging
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

class HazelcastProjectingTask(
    val queryId: String,
    val input: List<ByteArray>,
    val excludedServices: ByteArray,
    val qualifiedName: QualifiedName,
    val segment: Long
) : Callable<ByteArray>, Serializable, HazelcastInstanceAware {

    lateinit var localHazelcastInstance: HazelcastInstance

    override fun call(): ByteArray {

       val executorServiceStats = localHazelcastInstance.getExecutorService("executorService").localExecutorStats
       logger.info { "Task for queryId/segment ${queryId}/${segment} starting on node/endpoint ${localHazelcastInstance.name}/${localHazelcastInstance.localEndpoint} in cluster of [${localHazelcastInstance.cluster.members}] at time [${LocalDateTime.now()}] local executor = [${executorServiceStats}]" }

       val vyne = ApplicationContextProvider.context()!!.getBean("vyneFactory") as Vyne

       val context = QueryContext(
          facts = CopyOnWriteFactBag(CopyOnWriteArrayList(), vyne.schema),
          schema = vyne.schema,
          queryId = queryId,
          queryEngine = vyne.queryEngine(),
          profiler = QueryProfiler()
       )
        context.excludedServices.addAll( Cbor.decodeFromByteArray<MutableSet<SearchGraphExclusion<QualifiedName>>>(excludedServices) )

        val flow = input
           .asFlow()
           .map { SerializableTypedInstance.fromBytes(it) }  // Deserialize from CBor
           .map { it.toTypedInstance(vyne.schema) }
                .map {
                    GlobalScope.async {
                        val projectionContext = context.only(it)
                        val buildResult = projectionContext.build(qualifiedName)
                        buildResult.results.map { it.toSerializable().toBytes() to  SerializableVyneQueryStatistics.from(projectionContext.vyneQueryStatistics)  }
                    }
                }
                .buffer(16)
                .flatMapMerge { it.await() }.map { it  }

        //Run blocking is necessary here as the results need to be hydrated and serialised ByteArray
        return runBlocking {
            val list:List<Pair<ByteArray, SerializableVyneQueryStatistics>> = flow.toList()
            val encoded = Cbor.encodeToByteArray( list )
            encoded
        }
    }

    override fun setHazelcastInstance(hazelcastInstance: HazelcastInstance) {
        localHazelcastInstance = hazelcastInstance
    }
}

/**
 * A class to hold a reference to the spring context - populated when the spring context starts or changes
 *
 * The HazelcastProjectingTask is not a spring managed bean but needs access to spring beans - assumes the context
 * contains the necessary beans
 *
 */
@Component
class ApplicationContextProvider : ApplicationContextAware {

    @Throws(BeansException::class)
    override fun setApplicationContext(springApplicationContext: ApplicationContext?) {
       applicationContext = springApplicationContext
    }

    companion object {

        private var applicationContext: ApplicationContext? = null
        fun context():ApplicationContext? {
            return applicationContext
        }
    }
}
