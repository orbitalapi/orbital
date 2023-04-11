package io.vyne.connectors.config

import com.google.common.io.Resources
import io.kotest.matchers.shouldBe
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.config.kafka.KafkaConnection
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Assertions.*
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
      val fromBytes = Cbor.decodeFromByteArray<ConnectorsConfig>(bytes)
      fromBytes.shouldBe(config)
   }
   @Test
   fun `can read from disk`() {
      val path = Paths.get(
         Resources.getResource("mixed-connections.conf")
            .toURI()
      )
      val config = ConfigFileConnectorsRegistry(path).load()
      config.shouldBe(
         ConnectorsConfig(
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
