package io.vyne.testcontainers

import com.winterbe.expekt.should
import io.vyne.testcontainers.CommonSettings.CaskDefaultPort
import io.vyne.testcontainers.CommonSettings.EurekaServerDefaultPort
import io.vyne.testcontainers.CommonSettings.SchemaServerDefaultPort
import io.vyne.testcontainers.CommonSettings.VyneQueryServerDefaultPort
import org.apache.hc.client5.http.fluent.Request
import org.junit.jupiter.api.Test

class VyneContainerLongRunningTest {
   @Test
   @Throws(Exception::class)
   fun startVyneQueryServerInDistributedMode() {
      VyneContainerProvider
         .vyneQueryServer().use { vyneQueryServer ->
            vyneQueryServer
               .withDistributedPublicationMethod()
               .addExposedPort(VyneQueryServerDefaultPort)

            vyneQueryServer.start()
            val exposedPort = vyneQueryServer.firstMappedPort

            val response = Request.get("http://localhost:$exposedPort/api/security/config").execute().returnResponse()
            response.code.should.equals(200)
         }
   }

   @Test
   fun eurekaServer() {
      VyneContainerProvider
         .eureka().use { eureka ->
            eureka.addExposedPort(EurekaServerDefaultPort)
            eureka.start()
            val exposedPort = eureka.firstMappedPort
            val response = Request.get("http://localhost:$exposedPort/eureka/apps").execute().returnResponse()
            response.code.should.equals(200)
         }
   }

   @Test
   fun schemaServer() {
      VyneContainerProvider
         .schemaServer().use { schemaServer ->
            schemaServer.addExposedPort(SchemaServerDefaultPort)
            schemaServer.start()
            val exposedPort = schemaServer.firstMappedPort
            val response = Request.get("http://localhost:$exposedPort/actuator/health").execute().returnResponse()
            response.code.should.equals(200)
         }
   }

   @Test
   fun caskTest() {
      VyneContainerProvider
         .cask().use { cask ->
            cask
               .withDistributedPublicationMethod()
               .addExposedPort(CaskDefaultPort)
            cask.start()
            val exposedPort = cask.firstMappedPort
            val response = Request.get("http://localhost:$exposedPort/actuator/health").execute().returnResponse()
            response.code.should.equals(200)
         }
   }
}
