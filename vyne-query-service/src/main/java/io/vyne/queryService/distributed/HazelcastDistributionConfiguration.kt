package io.vyne.queryService.distributed

import com.hazelcast.core.HazelcastInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled


@Configuration
class HazelcastDistributionConfiguration(private val hazelcastInstance: HazelcastInstance) {
    val projectionQueue = hazelcastInstance.getQueue<String>("projectionQueue")

    init {

        println("Hazelcast instance name [${hazelcastInstance.name}]")

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                println("Consuming HZ message in coroutine")
                while (true) {
                    val item: String = projectionQueue.take()
                    println("Consumed: $item")
                }
            }
        }
    }
    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')


    fun distributeProjection(toDistribute:String) {
        projectionQueue.put(toDistribute)
    }

    @Scheduled(fixedDelay = 2000)
    fun publish() {

        println("Distibuting a string")
        val randomString = (1..10)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")

        distributeProjection(randomString)
    }


}