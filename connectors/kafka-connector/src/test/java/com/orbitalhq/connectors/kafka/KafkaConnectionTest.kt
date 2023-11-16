package com.orbitalhq.connectors.kafka

import com.winterbe.expekt.should
import com.orbitalhq.connectors.ConnectionSucceeded
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.kafka.registry.test
import com.orbitalhq.utils.get
import org.junit.Test
import java.time.Duration
import kotlin.random.Random

class KafkaConnectionTest : BaseKafkaContainerTest() {

   @Test
   fun `connection test returns successful with valid parameters`() {
      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
      )
      KafkaConnection.test(connection, timeout = Duration.ofSeconds(30)).get().should.equal(ConnectionSucceeded)
   }

   @Test
   fun `connection test returns error message with invalid bootstrap servers`() {
      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         "invalidHost:24601",
         "VyneTest-" + Random.nextInt(),
      )
      KafkaConnection.test(connection, timeout = Duration.ofSeconds(30))
         .get().should.equal("Failed to create new KafkaAdminClient : No resolvable bootstrap urls given in bootstrap.servers")
   }

}
