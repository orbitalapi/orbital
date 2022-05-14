package io.vyne.testcontainers

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import com.netflix.discovery.converters.XmlXStream
import com.netflix.discovery.shared.Applications
import org.rnorth.ducttape.timeouts.Timeouts
import org.rnorth.ducttape.unreliables.Unreliables
import java.util.concurrent.TimeUnit

class EurekaBasedSystemVerifier(
   private val retryCountLimit: Int = 5,
   private val waitInMillisecondsBetweenRetries: Long = 30000L,
   private val verifyPublishedSchemaBySchemaServer: Boolean = true) : VyneSystemVerifier {
   override fun verify(vyneSystem: VyneSystem) {
      val eurekaServer = vyneSystem.eurekaServer
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val exposedPort = eurekaServer.firstMappedPort
            val response = HttpClientBuilder.create().build().execute(HttpGet("http://localhost:$exposedPort/eureka/apps"))
            val xstream = XmlXStream()
            val eurekaApps = xstream.fromXML(response.entity.content) as Applications
            val vyneQueryServerEurekaApp =
               eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultQueryServerName }
            val schemaServerEurekaApp =
               eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultSchemaServerName }
            val caskServerEurekaApp =
               eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultCaskServerName }
            val pipelineOrchestratorApp =
               eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultPipelineOrchestratorName }
            val pipelineRunnerApp =
               eurekaApps.registeredApplications.firstOrNull { it.name == CommonSettings.defaultPipelineRunnerApp }
            val vyneAppsRegistered = vyneQueryServerEurekaApp != null && schemaServerEurekaApp != null &&
               caskServerEurekaApp != null && pipelineOrchestratorApp != null && pipelineRunnerApp != null
            if (!vyneAppsRegistered) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("Only ${eurekaApps.registeredApplications.map { it.name }} are registered to Eureka.")
            }
         }
      }
   }
}
