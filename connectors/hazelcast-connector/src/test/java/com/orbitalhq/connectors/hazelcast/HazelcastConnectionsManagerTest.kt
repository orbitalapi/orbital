package com.orbitalhq.connectors.hazelcast

import com.google.common.io.Resources
import com.hazelcast.core.Hazelcast
import com.hazelcast.test.TestHazelcastInstanceFactory
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.winterbe.expekt.should
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI

class HazelcastConnectionsManagerTest {
   @TempDir
   private lateinit var folder: File

   private fun buildRegistry(connectionConf: String): SourceLoaderConnectorsRegistry {
      val packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("com.test/foo/1.0.0")
      val testResourceUri = Resources.getResource(connectionConf).toURI()
      val configFilePath = testResourceUri.copyTo(folder).toPath()

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
         SimpleSchemaStore()
      )
         .hazelcastConnection(null)
      hzConfiguration.connectionName.should.equal("integrationHazelcast")
      hazelcast.shutdown()
   }

   @Test
   fun `should throw when requested connection name is null and there is no default hz connection`() {
      assertThrows<IllegalArgumentException> {
         HazelcastConnectionsManager(buildRegistry("hz-no-default-connection.conf"), SimpleSchemaStore())
            .hazelcastConnection(null)
      }
   }
}
