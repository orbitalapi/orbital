package io.vyne.connectors.kafka

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
abstract class BaseKafkaContainerTest {
   val hostName = "kafka"

   @Rule
   @JvmField
   final val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.2"))
      .withStartupTimeout(Duration.ofMinutes(2))
      .withNetworkAliases(hostName)

   @Before
   open fun before() {
      kafkaContainer.start()
      kafkaContainer.waitingFor(Wait.forListeningPort())
   }

   @After
   fun after() {
      kafkaContainer.stop()
   }
}
