package io.vyne.testcontainers

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import com.netflix.discovery.converters.XmlXStream
import com.netflix.discovery.shared.Application
import com.netflix.discovery.shared.Applications
import org.rnorth.ducttape.timeouts.Timeouts
import org.rnorth.ducttape.unreliables.Unreliables
import java.util.concurrent.TimeUnit

class EurekaBasedSystemVerifier(
   private val retryCountLimit: Int = 5,
   private val waitInMillisecondsBetweenRetries: Long = 30000L,
   private val verifyPublishedSchemaByFileSchemaServer: Boolean = true) : VyneSystemVerifier {
   override fun verify(vyneSystem: VyneSystem) {
      val eurekaServer = vyneSystem.eurekaServer
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val exposedPort = eurekaServer.firstMappedPort
            val response = HttpClientBuilder.create().build().execute(HttpGet("http://localhost:$exposedPort/eureka/apps"))
            val xstream = XmlXStream()
            val eurekaApps = xstream.fromXML(response.entity.content) as Applications
            val vyneQueryServerEurekaApp = eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultQueryServerName }
            val fileSchemaServerEurekaApp = eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultFileSchemaServerName }
            val caskServerEurekaApp = eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultCaskServerName }
            val vyneAppsRegistered = vyneQueryServerEurekaApp != null && fileSchemaServerEurekaApp != null &&
               caskServerEurekaApp != null && verifyFileSchemaServerPublishedInitialSchema(fileSchemaServerEurekaApp)
            if (!vyneAppsRegistered) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("${eurekaApps.registeredApplications.map { it.name }} are only registered to Eureka")
            }
         }
      }
   }

   private fun verifyFileSchemaServerPublishedInitialSchema(fileSchemaServer: Application): Boolean {
      return if (this.verifyPublishedSchemaByFileSchemaServer) {
         val metadataPublishedByFileSchemaServer = fileSchemaServer.instances.first().metadata
         metadataPublishedByFileSchemaServer.keys.any { key -> key.contains("vyne.sources.") }
      } else {
         true
      }
   }
}
