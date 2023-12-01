package com.orbitalhq.spring.config

import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import com.orbitalhq.config.toHocon
import com.orbitalhq.http.ServicesConfig
import com.orbitalhq.http.ServicesConfigRepository
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText


class FileBasedDiscoveryClientTest {
   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()

   @Test
   fun `will resolve env variables in config file`() {
      val configFile = tempFolder.root.resolve("services.conf").toPath()
      // Kotlin string interpolation in triple-quotes makes it really hard to do ${VYNE_HOST}
      val VYNE_HOST = "\${VYNE_HOST}"
      val configText = """services {
    query-server {
     url: "http://"$VYNE_HOST":9090"
    }
}
"""
      configFile.writeText(
         configText
      )
      val stubEnvConfig = mapOf("VYNE_HOST" to "localhost").toHocon()
      val configRepository = ServicesConfigRepository(configFile,stubEnvConfig)
      val client = FileBasedDiscoveryClient(configRepository)
      val queryServer = client.getInstances("query-server").single()
      queryServer.uri.toASCIIString().should.equal("http://localhost:9090")
   }

   @Test
   fun `if config file doesnt exist then default file is written`() {
      val configFile = tempFolder.root.resolve("services.conf").toPath()
      Files.exists(configFile).should.be.`false`

      // Create the client, which should generate a default file
      val client = FileBasedDiscoveryClient(configFile)

      Files.exists(configFile).should.be.`true`
      client.services.should.have.size(ServicesConfig.DEFAULT.services.size)
      val schemaServer = client.getInstances("schema-server").first()
      val rsocket = schemaServer.metadata["rsocket"]
      rsocket.shouldBe("tcp://schema-server:7655")
   }

   @Test
   fun `when watching file changes are detected`() {
      val configFile = tempFolder.root.resolve("services.conf").toPath()
      configFile.writeText(
         """services {
    query-server {
     url="http://vyne"
    }
}
"""
      )
      val client = FileBasedDiscoveryClient(configFile)

      client.services.should.have.size(1)

//      client.watchForChanges()

      // Wait a bit for everything to register
      Thread.sleep(500)

      configFile.writeText(
         """services {
    query-server {
      url="http://vyne"
    }
    another-service {
      url="http://foo"
    }
}
"""
      )
      Awaitility.await().atMost(3, TimeUnit.SECONDS).until<Boolean> {
         client.services.size == 2
      }
   }
}

