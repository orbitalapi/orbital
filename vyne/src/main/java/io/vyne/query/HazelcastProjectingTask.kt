package io.vyne.query

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.HazelcastInstanceAware
import io.vyne.Vyne
import io.vyne.models.SerializableTypedInstance
import io.vyne.models.TypedInstance
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
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList

class HazelcastProjectingTask(
    val queryId: String,
    val input: List<ByteArray>,
    val qualifiedName: QualifiedName
) : Callable<List<Pair<TypedInstance, VyneQueryStatistics>>>, Serializable, HazelcastInstanceAware {

    var localHazelcastInstance: HazelcastInstance? = null

    override fun call(): List<Pair<TypedInstance, VyneQueryStatistics>> {

        val vyne = ApplicationContextProvider!!.context()!!.getBean("vyne") as Vyne
        val queryEngine = QueryEngineFactory.default().queryEngine(vyne.schema)
        val context = QueryContext(facts = CopyOnWriteArrayList(), schema = vyne.schema, queryId = queryId, queryEngine = queryEngine, profiler =  QueryProfiler())
            val flow = input
                .asFlow()
                .map{ Cbor.decodeFromByteArray<SerializableTypedInstance>(it) }  //Deserialize from CBor
                .map { it.toTypedInstance(vyne.schema) }
                .map {
                    GlobalScope.async {
                        val projectionContext = context.only(it)
                        val buildResult = projectionContext.build(qualifiedName)
                        buildResult.results.map { it to projectionContext.vyneQueryStatistics }
                    }
                }
                .buffer(16)
                .flatMapMerge { it.await() }
            return runBlocking { flow.toList() }
    }

    override fun setHazelcastInstance(hazelcastInstance: HazelcastInstance?) {
        localHazelcastInstance = hazelcastInstance
    }
}


@Component
class ApplicationContextProvider : ApplicationContextAware {

    @Throws(BeansException::class)
    override fun setApplicationContext(springApplicationContext: ApplicationContext?) {
        ApplicationContextProvider.applicationContext = springApplicationContext
    }

    companion object {

        private var applicationContext: ApplicationContext? = null
        fun context():ApplicationContext? {
            return applicationContext
        }
    }
}