package io.vyne.query

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.HazelcastInstanceAware
import io.vyne.models.TypeNamedInstance
import io.vyne.models.TypedInstance
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Callable
import java.io.Serializable
import java.util.concurrent.Executors

private val projectingExecutorService = Executors.newFixedThreadPool(64)

class HazelcastProjectingTask(
    val context: QueryContext,
    val input: List<TypedInstance>
) : Callable< List<Pair<TypedInstance, VyneQueryStatistics>> >, Serializable, HazelcastInstanceAware {

    var localHazelcastInstance: HazelcastInstance? = null

    override fun call(): List<Pair<TypedInstance, VyneQueryStatistics>> {

        val callableTask: Callable<List<Pair<TypedInstance, VyneQueryStatistics>>> = Callable<List<Pair<TypedInstance, VyneQueryStatistics>>> {
            val flow = input
                .asFlow()
                .map {
                    GlobalScope.async {
                        val actualProjectedType = context.projectResultsTo?.collectionType ?: context.projectResultsTo
                        val projectionContext = context.only(it)
                        val buildResult = projectionContext.build(actualProjectedType!!.qualifiedName)
                        buildResult.results.map { it to projectionContext.vyneQueryStatistics }
                    }
                }
                .buffer(16)
                .flatMapMerge { it.await() }
            runBlocking { flow.toList() }
        }
        return projectingExecutorService.submit(callableTask).get()

    }

    override fun setHazelcastInstance(hazelcastInstance: HazelcastInstance?) {
        localHazelcastInstance = hazelcastInstance
    }
}

class EchoTask(
    val input: String
) : Callable< String >, Serializable, HazelcastInstanceAware {

    var localHazelcastInstance: HazelcastInstance? = null

    override fun call(): String {
        return localHazelcastInstance?.getCluster()?.getLocalMember().toString() + ":" + input
    }

    override fun setHazelcastInstance(hazelcastInstance: HazelcastInstance?) {
        localHazelcastInstance = hazelcastInstance
    }
}


class InstanceTask(
    val input: List<LinkedHashMap<*,*>>
) : Callable< String >, Serializable, HazelcastInstanceAware {

    var localHazelcastInstance: HazelcastInstance? = null

    override fun call(): String {
        return localHazelcastInstance?.getCluster()?.getLocalMember().toString() + ":" + "instance" + input.size
    }

    override fun setHazelcastInstance(hazelcastInstance: HazelcastInstance?) {
        localHazelcastInstance = hazelcastInstance
    }
}