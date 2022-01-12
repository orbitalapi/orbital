package io.vyne.support

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName

object KafkaInitialiser {
   const val hostName = "kafka"
   fun initialiseKafka(vyneNetwork: Network): KafkaContainer {
      return KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
         .withNetwork(vyneNetwork)
         .withNetworkAliases(hostName)
   }

}
