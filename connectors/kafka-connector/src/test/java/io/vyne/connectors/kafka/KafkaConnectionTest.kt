package io.vyne.connectors.kafka

import com.winterbe.expekt.should
import io.vyne.connectors.ConnectionSucceeded
import io.vyne.utils.get
import org.junit.Test
import kotlin.random.Random

class KafkaConnectionTest : BaseKafkaContainerTest() {

   @Test
   fun `connection test returns successful with valid parameters`() {
      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         kafkaContainer.bootstrapServers,
         "VyneTest-" + Random.nextInt(),
      )
      KafkaConnection.test(connection).get().should.equal(ConnectionSucceeded)
   }

   @Test
   fun `connection test returns error message with invalid bootstrap servers`() {
      val connection = KafkaConnectionConfiguration(
         "moviesConnection",
         "invalidHost:24601",
         "VyneTest-" + Random.nextInt(),
      )
      KafkaConnection.test(connection)
         .get().should.equal("Failed to construct kafka consumer : No resolvable bootstrap urls given in bootstrap.servers")
   }

}
