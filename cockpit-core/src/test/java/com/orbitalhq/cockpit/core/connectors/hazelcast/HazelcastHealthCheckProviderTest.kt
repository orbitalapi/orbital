package com.orbitalhq.cockpit.core.connectors.hazelcast

import com.hazelcast.client.test.TestHazelcastFactory
import com.hazelcast.core.HazelcastInstance
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import com.orbitalhq.connectors.registry.ConnectionStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import reactor.test.StepVerifier


class HazelcastHealthCheckProviderTest: DescribeSpec({
   describe("Hazelcast operation cache") {
      val hazelcastInstanceFactory = TestHazelcastFactory()
      hazelcastInstanceFactory.newHazelcastInstance()
      val host = hazelcastInstanceFactory.knownAddresses.first().host
      val port = hazelcastInstanceFactory.knownAddresses.first().port
      val hazelcastConfiguration = HazelcastConfiguration(
         connectionName = "testConnection",
         addresses = listOf("$host:$port"))

      it("should check health") {
         val hazelcastHealthCheckProvider = HazelcastHealthCheckProvider(object : HazelcastInstanceProvider {
            override fun provide(config: HazelcastConfiguration): HazelcastInstance {
               return hazelcastInstanceFactory.newHazelcastClient()
            }
         })
         hazelcastHealthCheckProvider.canProvideFor(hazelcastConfiguration).shouldBeTrue()
         StepVerifier.create(
            hazelcastHealthCheckProvider
               .provide(hazelcastConfiguration)
         ).assertNext { connectionStatus ->
            connectionStatus.status.shouldBe(ConnectionStatus.Status.OK)
         }.verifyComplete()
         hazelcastInstanceFactory.shutdownAll()
      }
   }
})
