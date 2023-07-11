package io.vyne.connectors.config

import com.google.common.io.Resources
import io.kotest.matchers.maps.shouldHaveKeys
import io.kotest.matchers.shouldBe
import io.vyne.config.FileConfigSourceLoader
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ConfigFileConnectorsRegistryTest {

   @OptIn(ExperimentalSerializationApi::class)
   @Test
   fun `can serde to cbor`() {
      val path = Paths.get(
         Resources.getResource("mixed-connections.conf")
            .toURI()
      )
      val config = ConfigFileConnectorsRegistry(path).load()
      val bytes = Cbor.encodeToByteArray(config)
      val fromBytes = Cbor.decodeFromByteArray<ConnectionsConfig>(bytes)
      fromBytes.shouldBe(config)
   }

   @Test
   fun `will merge multiple configs`() {
      val path1 = Paths.get(
         Resources.getResource("mixed-connections.conf")
            .toURI()
      )
      val path2 = Paths.get(
         Resources.getResource("connections-2.conf")
            .toURI()
      )
      val config = ConfigFileConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(path1),
            FileConfigSourceLoader(path2),
         )
      ).load()
      config.jdbc.shouldHaveKeys("another-connection", "connection-2", "connection-3", "connection-4")
      config.kafka.shouldHaveKeys("kafka-connection", "kafka-connection-2")
   }

   @Test
   fun `can read from disk`() {
      val path = Paths.get(
         Resources.getResource("mixed-connections.conf")
            .toURI()
      )
      val config = ConfigFileConnectorsRegistry(path).load()
      config.shouldBe(
         ConnectionsConfig(
            jdbc = mapOf(
               "another-connection" to DefaultJdbcConnectionConfiguration(
                  connectionName = "another-connection",
                  connectionParameters = mapOf(
                     "database" to "transactions",
                     "host" to "our-db-server",
                     "password" to "super-secret",
                     "port" to "2003",
                     "username" to "jack"
                  ),
                  jdbcDriver = JdbcDriver.POSTGRES
               ),
               "connection-2" to DefaultJdbcConnectionConfiguration(
                  connectionName = "connection-2",
                  connectionParameters = mapOf(
                     "database" to "transactions",
                     "host" to "our-db-server",
                     "password" to "super-secret",
                     "port" to "2003",
                     "username" to "jack"
                  ),
                  jdbcDriver = JdbcDriver.POSTGRES
               )
            ),
            kafka = mapOf(
               "kafka-connection" to KafkaConnectionConfiguration(
                  connectionName = "kafka-connection",
                  connectionParameters = mapOf(
                     "brokers" to "localhost:29092,localhost:39092",
                     "offset" to "earliest",
                     "topic" to "oldmovies"
                  )
               )
            )
         )

      )
   }

}
