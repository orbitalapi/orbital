package io.vyne.connectors.jdbc.registry

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.config.FileConfigSourceLoader
import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class SourceLoaderJdbcConnectionRegistryTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   val defaultPackageIdentifier = PackageIdentifier.fromId("com.test/foo/1.0.0")

   private fun configFileInTempFolder(resourceName: String): Path {
      return Resources.getResource(resourceName).toURI()
         .copyTo(folder.root)
         .toPath()
   }

   @Test
   fun `can write jdbc connection to new config file`() {
      val configFile = folder.root.toPath().resolve("connections.conf")
      val registry = buildRegistry(configFile)
      val connection = DefaultJdbcConnectionConfiguration.forParams(
         "test-db-connection",
         JdbcDriver.POSTGRES,
         connectionParameters = mapOf(
            PostgresJdbcUrlBuilder.Parameters.HOST to "localhost",
//            PostgresJdbcUrlBuilder.Parameters.PORT to "localhost", // omit port, so it defaults
            PostgresJdbcUrlBuilder.Parameters.DATABASE to "pets",
            PostgresJdbcUrlBuilder.Parameters.USERNAME to "jimmy",
            PostgresJdbcUrlBuilder.Parameters.PASSWORD to "password",
         )
      )
      registry.register(defaultPackageIdentifier, connection)
      val written = configFile.toFile().readText()
      val readingRegistry = buildRegistry(configFile)
      val readFromDisk = readingRegistry.listAll()
      readFromDisk.should.have.size(1)
      readFromDisk[0].should.equal(connection)
   }

   private fun buildRegistry(
      configFile: Path,
      packageIdentifier: PackageIdentifier = defaultPackageIdentifier
   ): SourceLoaderJdbcConnectionRegistry {

      val actualRegistry = SourceLoaderConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(
               configFile,
               packageIdentifier = packageIdentifier,
               failIfNotFound = false
            )
         )
      )
      return SourceLoaderJdbcConnectionRegistry(
         actualRegistry
      )
   }

   @Test
   fun `can append jdbc connection to existing config file`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = buildRegistry(configFile)
      val connection = DefaultJdbcConnectionConfiguration.forParams(
         "third-connection",
         JdbcDriver.POSTGRES,
         connectionParameters = mapOf(
            PostgresJdbcUrlBuilder.Parameters.HOST to "our-third-db-server",
            PostgresJdbcUrlBuilder.Parameters.PORT to "2003",
            PostgresJdbcUrlBuilder.Parameters.DATABASE to "transactions",
            PostgresJdbcUrlBuilder.Parameters.USERNAME to "jack",
            PostgresJdbcUrlBuilder.Parameters.PASSWORD to "super-secret",
         )
      )
      registry.register(defaultPackageIdentifier, connection)
      val written = configFile.toFile().readText()
      val readingRegistry = buildRegistry(configFile)
      readingRegistry.listAll().should.have.size(3)
      val readFromDisk = readingRegistry.getConnection("third-connection")
      readFromDisk.should.equal(connection)
   }

   @Test
   fun `can remove jdbc connection from config file registry`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = buildRegistry(configFile)
      registry.listAll().should.have.size(2)
      registry.hasConnection("another-connection").should.be.`true`

      registry.remove(defaultPackageIdentifier, "another-connection")
      registry.listAll().should.have.size(1)
      registry.hasConnection("another-connection").should.be.`false`

      val readingRegistry = buildRegistry(configFile)
      readingRegistry.listAll().should.have.size(1)
      readingRegistry.hasConnection("another-connection").should.be.`false`
   }

   @Test
   fun `can load jdbc connection from config file registry`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val registry = buildRegistry(configFile)
      registry.listAll().should.have.size(2)
      registry.hasConnection("another-connection").should.be.`true`
      registry.hasConnection("second-db-connection").should.be.`true`
   }

   @Test
   fun `when editing config file with other connection types then only jdbc values are modified`() {
      val configFile = configFileInTempFolder("config/mixed-connections.conf")
      val registry = buildRegistry(configFile)
      registry.listAll().should.have.size(1)
      registry.hasConnection("another-connection").should.be.`true`

      registry.remove(defaultPackageIdentifier, "another-connection")
      registry.listAll().should.have.size(0)
      val configFileContents = configFile.toFile().readText()
      // Ensure the kafka settings remain untouched...
      configFileContents.should.equal(
         """jdbc {}
kafka {
    some-connection {
        connectionName=some-connection
        connectionParameters {}
    }
}
"""
      )
   }

}

