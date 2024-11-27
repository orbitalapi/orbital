package com.orbitalhq.connectors.hazelcast

import com.google.common.io.Resources
import com.hazelcast.client.test.TestHazelcastFactory
import com.hazelcast.core.Hazelcast
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.registry.ConfigurationFilePathCustomType
import com.winterbe.expekt.should
import io.github.config4k.registerCustomType
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI

class HazelcastConnectionsManagerTest {
   @TempDir
   private lateinit var folder: File

   private fun buildRegistry(connectionConf: String, additionalConfigFiles: List<String> =  emptyList()): SourceLoaderConnectorsRegistry {
      val packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("com.test/foo/1.0.0")
      val testResourceUri = Resources.getResource(connectionConf).toURI()
      val configFilePath = testResourceUri.copyTo(folder).toPath()

      additionalConfigFiles.forEach {
         Resources.getResource(it).toURI().copyTo(folder)
      }

      return SourceLoaderConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(
               configFilePath,
               packageIdentifier = packageIdentifier,
               failIfNotFound = false
            )
         )
      )

   }



   fun URI.copyTo(destDirectory: File): File {
      val source = File(this)
      val destFile = destDirectory.resolve(source.name)
      FileUtils.copyFile(source, destFile)
      return destFile
   }

   @Test
   fun `should return hz connection with default attribute when requested connection name is null`() {
      val hazelcast = Hazelcast.newHazelcastInstance()
      val (_, hzConfiguration) = HazelcastConnectionsManager(
         buildRegistry("hz-default-connection.conf"),
         hazelcast
      )
         .hazelcastConnection(null)
      hzConfiguration.connectionName.should.equal("integrationHazelcast")
      hazelcast.shutdown()
   }

   @Test
   fun `should throw with multiple default hz connections`() {
      assertThrows<IllegalArgumentException> {
         HazelcastConnectionsManager(
            buildRegistry("hz-multiple-default-connections.conf"),
            TestHazelcastFactory().newHazelcastInstance()
         ).hazelcastConnection(null)
      }
   }

   @Test
   fun `should return embedded instance when configType is embedded`() {
      val hazelcast = Hazelcast.newHazelcastInstance()
      val (hzInstance, hzConfiguration) = HazelcastConnectionsManager(
         buildRegistry("hz-connections-with-embedded.conf"),
         hazelcast
      )
         .hazelcastConnection("integrationHazelcast")
      hzConfiguration.connectionName.should.equal("integrationHazelcast")
      hzInstance.should.equal(hazelcast)
      hazelcast.shutdown()

   }

   @Test
   fun `should return hz connection from xml configuration`() {
      val hazelcast = Hazelcast.newHazelcastInstance()
      val (_, hzConfiguration) = HazelcastConnectionsManager(
         buildRegistry("hz-default-connection.conf", listOf("hazelcast-client.xml")),
         hazelcast
      )
         .hazelcastConnection("clientWithXmlConfig")
      hzConfiguration.connectionName.should.equal("clientWithXmlConfig")
      hazelcast.shutdown()
   }

   @Test
   fun `should return hz connection from yaml configuration`() {
      val hazelcast = Hazelcast.newHazelcastInstance()
      val (_, hzConfiguration) = HazelcastConnectionsManager(
         buildRegistry("hz-default-connection.conf", listOf("hazelcast-client.yaml")),
         hazelcast
      )
         .hazelcastConnection("clientWithYamlConfig")
      hzConfiguration.connectionName.should.equal("clientWithYamlConfig")
      hazelcast.shutdown()
   }

   @Test
   fun `should throw when requested connection name is null and there is no default hz connection`() {
      assertThrows<IllegalStateException> {
         HazelcastConnectionsManager(buildRegistry("hz-no-default-connection.conf"),
            TestHazelcastFactory().newHazelcastInstance())
            .hazelcastConnection(null)
      }
   }

   companion object {
      @JvmStatic
      @BeforeAll
      fun setup() {
         registerCustomType(ConfigurationFilePathCustomType())
      }
   }
}
