package com.orbitalhq.nebula

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.nebula.core.NebulaStackState
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemaServer.core.config.SchemaUpdateNotifier
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NebulaEnvVariableSourceTest {
   @TempDir
   lateinit var tempDir: File

   @Test
   fun `creates environment variables`() {

      val envVariables = NebulaEnvVariableSource.extractVariables(nebulaStackState())
      envVariables.shouldBe(
         mapOf(
            "NEBULA_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
            "NEBULA_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
            "NEBULA_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
            "NEBULA_COM_TEST_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",

            "NEBULA_HTTP_PORT" to "38551",
            "NEBULA_SERVICES_HTTP_PORT" to "38551",
            "NEBULA_HELLO_WORLD_SERVICES_HTTP_PORT" to "38551",
            "NEBULA_COM_TEST_HELLO_WORLD_SERVICES_HTTP_PORT" to "38551",
         )
      )
   }

   @Test
   fun `lists env variables by component`() {
      val envVariables = NebulaEnvVariableSource.getVariablesByStackComponent(nebulaStackState())
      envVariables.shouldBe(
         mapOf(
            "[com.test:hello-world]/services" to mapOf(
               "http" to mapOf(
                  "NEBULA_HTTP_PORT" to "38551",
                  "NEBULA_SERVICES_HTTP_PORT" to "38551",
                  "NEBULA_HELLO_WORLD_SERVICES_HTTP_PORT" to "38551",
                  "NEBULA_COM_TEST_HELLO_WORLD_SERVICES_HTTP_PORT" to "38551"
               ),
               "kafka" to mapOf(
                  "NEBULA_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
                  "NEBULA_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
                  "NEBULA_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
                  "NEBULA_COM_TEST_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS" to "PLAINTEXT://localhost:49187",
               )
            ),

            )
      )
   }

   @Test
   fun `connections with variables are resolved using nebula env vars`() {
      val config = """
         kafka {
            my-kafka {
               connectionName=my-kafka
               connectionParameters {
                  brokerAddress=${'$'}{NEBULA_KAFKA_BOOTSTRAP_SERVERS}
                  groupId=vyne
               }
            }
         }
      """.trimIndent()
      val connectionsConf = tempDir.resolve("connections.conf")
      connectionsConf.writeText(config)
      val schemaUpdateNotifier = mock<SchemaChangedEventProvider>()

      val nebulaLoader = NebulaEnvVariableSource(schemaUpdateNotifier)
      val fileLoader = FileConfigSourceLoader(
         connectionsConf.toPath(),
         packageIdentifier = PackageIdentifier.fromId("foo/test/1.0.0")
      )

      // Provide the current stack state to the env variable loader,
      // which should publish env variables
      nebulaLoader.updateEnvVariables(nebulaStackState())

      val connectorsRegistry = SourceLoaderConnectorsRegistry(
         listOf(fileLoader, nebulaLoader)
      )

      val c = connectorsRegistry.load()
      c.kafka.shouldHaveSize(1)
      c.kafka.values.single().connectionParameters["brokerAddress"]!!.shouldBe("PLAINTEXT://localhost:49187")

      // verify that an update event was sent
      verify(schemaUpdateNotifier).forceSchemaChangedEvent()
   }


   private fun nebulaStackState(kafkaPort: Int = 49187): NebulaStackState {
      return jacksonObjectMapper().readValue<NebulaStackState>(
         """{
    "[com.test:hello-world]/services": {
        "http": {
            "container": null,
            "componentConfig": {
                "port": 38551
            }
        },
        "kafka": {
            "container": {
                "containerId": "ca7b58f60ab89da23935bbd278fbe02401656d515198241da3715cda00318872",
                "imageName": "confluentinc/cp-kafka:6.2.2",
                "containerName": "/nostalgic_ramanujan"
            },
            "componentConfig": {
                "bootstrapServers": "PLAINTEXT://localhost:$kafkaPort"
            }
        }
    }
}"""
      )
   }
}
