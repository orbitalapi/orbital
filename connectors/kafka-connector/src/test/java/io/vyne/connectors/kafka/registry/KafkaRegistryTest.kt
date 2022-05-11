package io.vyne.connectors.kafka.registry

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.utils.withoutWhitespace
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.nio.file.Path

class KafkaRegistryTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `can write kafka connection to new config file`() {
      val configFile = folder.root.toPath().resolve("connections.conf")
      val registry = KafkaConfigFileConnectorRegistry(configFile)
      val connection = KafkaConnectionConfiguration(
         "test-kafka-connection",
         "localhost:9092",
         "vyne"
      )
      registry.saveConnectorConfig(connection)
      val written = configFile.toFile().readText()
      written.withoutWhitespace().should.equal("""kafka {
    test-kafka-connection {
        connectionName=test-kafka-connection
        connectionParameters {
            brokerAddress="localhost:9092"
            groupId=vyne
        }
    }
}""".withoutWhitespace())
      val readingRegistry = KafkaConfigFileConnectorRegistry(configFile)
      val readFromDisk = readingRegistry.listConnections()
      readFromDisk.should.have.size(1)
      readFromDisk[0].should.equal(connection)
   }

   @Test
   fun `can append kafka connection to existing config file`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = KafkaConfigFileConnectorRegistry(configFile)
      val connection = KafkaConnectionConfiguration(
         "third-connection",
         "localhost:9092",
         "vyne",
      )
      registry.saveConnectorConfig(connection)
      val written = configFile.toFile().readText()
      val readingRegistry = KafkaConfigFileConnectorRegistry(configFile)
      readingRegistry.listConnections().should.have.size(3)
      val readFromDisk = readingRegistry.getConnection("third-connection")
      readFromDisk.should.equal(connection)
   }

   @Test
   fun `can remove kafka connection from config file registry`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = KafkaConfigFileConnectorRegistry(configFile)
      registry.listConnections().should.have.size(2)
      registry.hasConnection("another-connection").should.be.`true`

      registry.removeConnectorConfig("another-connection")
      registry.listConnections().should.have.size(1)
      registry.hasConnection("another-connection").should.be.`false`

      val readingRegistry = KafkaConfigFileConnectorRegistry(configFile)
      readingRegistry.listConnections().should.have.size(1)
      readingRegistry.hasConnection("another-connection").should.be.`false`
   }

   @Test
   fun `can load kafka connection from config file registry`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = KafkaConfigFileConnectorRegistry(configFile)
      registry.listConnections().should.have.size(2)
      registry.hasConnection("another-connection").should.be.`true`
      registry.hasConnection("second-db-connection").should.be.`true`
   }


   private fun configFileInTempFolder(resourceName: String): Path {
      return Resources.getResource(resourceName).toURI()
         .copyTo(folder.root)
         .toPath()
   }
}


fun URI.copyTo(destDirectory: File): File {
   val source = File(this)
   val destFile = destDirectory.resolve(source.name)
   FileUtils.copyFile(source, destFile)
   return destFile
}
