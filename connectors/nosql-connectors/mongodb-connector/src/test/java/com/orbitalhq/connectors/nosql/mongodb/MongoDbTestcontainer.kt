package com.orbitalhq.connectors.nosql.mongodb

import mu.KotlinLogging
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

private val logger = KotlinLogging.logger {  }

@Testcontainers
abstract class MongoDbTestcontainer {
   companion object {
      @Container
      val mongoDbContainer: MongoDbContainer = MongoDbContainer()
         .withExposedPorts(27017)
//         .withCommand("--replSet", "rs0", "--bind_ip_all")  // <-- run in replica set mode, to allow transactions
         .withLogConsumer {
            Slf4jLogConsumer(logger)
         }
         .withCopyToContainer(
            MountableFile.forClasspathResource("./init-schema.js"),
            "/docker-entrypoint-initdb.d/init-script.js"
         ).apply { start() }
//         .also { container ->
//            // Initialize replica set once container is up
//            // Required to allow transactions
//            val result = container.execInContainer(
//               "mongosh", "--eval", "rs.initiate()"
//            )
//            if (result.exitCode != 0) {
//               error("Failed to initiate replica set: ${result.stderr}")
//            }
//         }

      val connectionString: String
         get() {
         return "mongodb://test_container:test_container@${mongoDbContainer.host}:${mongoDbContainer.firstMappedPort}/user_management"
      }

      val connectionStringWithInvalidPassword: String
         get() {
            return "mongodb://test_container:pass@${mongoDbContainer.host}:${mongoDbContainer.firstMappedPort}/user_management"
         }

      val connectionStringWithInvalidPort: String
         get() {
            return "mongodb://test_container:pass@${mongoDbContainer.host}:${mongoDbContainer.firstMappedPort.plus(1)}/user_management"
         }
   }
}

   class MongoDbContainer: GenericContainer<MongoDbContainer>(DockerImageName.parse("mongo:6.0.7"))
