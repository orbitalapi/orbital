package io.vyne.testcontainers

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.EurekaInstanceConfig
import com.netflix.appinfo.MyDataCenterInstanceConfig
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider
import com.netflix.discovery.DefaultEurekaClientConfig
import com.netflix.discovery.DiscoveryClient
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.EurekaClientConfig
import com.netflix.discovery.converters.XmlXStream
import com.netflix.discovery.shared.Applications
import com.thoughtworks.xstream.XStream
import io.vyne.testcontainers.CommonSettings.EurekaServerDefaultPort
import io.vyne.testcontainers.CommonSettings.defaultCaskServerName
import io.vyne.testcontainers.CommonSettings.defaultFileSchemaServerName
import io.vyne.testcontainers.CommonSettings.defaultQueryServerName
import io.vyne.testcontainers.CommonSettings.eurekaServerUri
import io.vyne.testcontainers.CommonSettings.fileSchemaServerSchemaPath
import io.vyne.testcontainers.CommonSettings.latest
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.rnorth.ducttape.timeouts.Timeouts
import org.rnorth.ducttape.unreliables.Unreliables
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Network
import org.testcontainers.images.ImagePullPolicy
import org.testcontainers.images.PullPolicy
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.MountableFile
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit


data class VyneSystem(
   val eurekaServer: VyneContainer,
   val vyneQueryServer: VyneContainer,
   val fileSchemaServer: VyneContainer,
   val caskServer: VyneContainer,
   val pipelineOrchestrator: VyneContainer,
   val pipelineRunner: VyneContainer,
   val network: Network) : AutoCloseable {

   fun start(vyneSystemVerifier: VyneSystemVerifier = EurekaBasedSystemVerifier()) {
      eurekaServer.start()
      vyneQueryServer.start()
      fileSchemaServer.start()
      caskServer.start()
      pipelineOrchestrator.start()
      pipelineRunner.start()
      vyneSystemVerifier.verify(this)
   }

   fun ensurePipelineRunner(retryCountLimit: Int = 5,
                            waitInMillisecondsBetweenRetries: Long = 30000L) {
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val response = Request
               .get("http://localhost:${this.pipelineOrchestrator.firstMappedPort}/api/runners")
               .setHeader("Content-Type", "application/json")
               .execute()
            if (response.returnResponse().code != 200) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("no runners found!")
            }

            val pipelinesResponse = response.returnContent().asString()
            if (pipelinesResponse == null || !pipelinesResponse.contains("instanceId")) {
               throw IllegalStateException("no runners found!")
            }
         }
      }
   }

   fun createPipeline(pipelineDefinitionJson: InputStream): Int {
      val request = Request
         .post("http://localhost:${this.pipelineOrchestrator.firstMappedPort}/api/runners")
         .bodyStream(pipelineDefinitionJson)
         .setHeader("Accept", "application/json, text/javascript, */*")
         .setHeader("Content-Type", "application/json")

      return request.execute().returnResponse().code
   }

   fun isPipelineRunning(retryCountLimit: Int = 5,
                         waitInMillisecondsBetweenRetries: Long = 30000L) {
      Unreliables.retryUntilSuccess(retryCountLimit) {
         Timeouts.doWithTimeout(1, TimeUnit.MINUTES) {
            val response = Request
               .get("http://localhost:${this.pipelineOrchestrator.firstMappedPort}/api/pipelines")
               .setHeader("Content-Type", "application/json")
               .execute()
            if (response.returnResponse().code != 200) {
               Thread.sleep(waitInMillisecondsBetweenRetries)
               throw IllegalStateException("no runners found!")
            }

            val pipelinesResponse = response.returnContent().asString()
            if (pipelinesResponse == null || !pipelinesResponse.contains("RUNNING")) {
               throw IllegalStateException("no pipeline is running!")
            }
         }
      }
   }

   companion object {
      /**
       * Creates a Vyne System consisting of Eureka, Vyne Query Server, File Schema Server and Cask docker images for the given docker image tag.
       * File schema server is configured to read the core schema from the folder on your 'local' host and the folder path is specified by [schemaSourceDirectoryPath] folder.
       * @param schemaSourceDirectoryPath Taxi Schema Folder on your 'local' host containing taxi files
       * @param tag Docker Image tag. When not provided it is set to latest
       * @param alwaysPullImages Flag to force pulling docker images. By default it is false so container images are always retrieved from local Docker Image cache.
       */
      fun withEurekaAndFileBasedSchema(
         schemaSourceDirectoryPath: String? = null,
         tag: DockerImageTag = latest,
         alwaysPullImages: Boolean = false): VyneSystem {
         val vyneNetwork = Network.newNetwork()
         val eurekaNetworkAlias = "eureka"
         val eurekaUri = "http://$eurekaNetworkAlias:$EurekaServerDefaultPort"
         val eureka = VyneContainerProvider.eureka(tag) {
            addExposedPort(EurekaServerDefaultPort)
            withNetworkAliases(eurekaNetworkAlias)
            withNetwork(vyneNetwork)
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }

         val vyneQueryServer = VyneContainerProvider.vyneQueryServer(tag) {
            withEurekaPublicationMethod()
            withInMemoryQueryHistory()
            addExposedPort(CommonSettings.VyneQueryServerDefaultPort)
            withNetwork(vyneNetwork)
            withOption("$eurekaServerUri=$eurekaUri")
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }

         val fileSchemaServer = VyneContainerProvider.fileSchemaServer(tag) {
            schemaSourceDirectoryPath?.let {
               withFileSystemBind(it, "/tmp/schema", BindMode.READ_WRITE)
            }
            withOption("$fileSchemaServerSchemaPath=/tmp/schema")
            withEurekaPublicationMethod()
            addExposedPort(CommonSettings.FileSchemaServerDefaultPort)
            withNetwork(vyneNetwork)
            withOption("$eurekaServerUri=$eurekaUri")
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }

         val cask = VyneContainerProvider.cask(tag) {
            withEurekaPublicationMethod()
            addExposedPort(CommonSettings.CaskDefaultPort)
            withNetwork(vyneNetwork)
            withOption("$eurekaServerUri=$eurekaUri")
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }

         val pipelineOrchestrator = VyneContainerProvider.pipelineOrchestrator(tag) {
            addExposedPort(CommonSettings.PipelineOrchestratorDefaultPort)
            withNetwork(vyneNetwork)
            withOption("$eurekaServerUri=$eurekaUri")
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }

         val pipelineRunnerApp = VyneContainerProvider.pipelineRunnerApp(tag) {
            withEurekaPublicationMethod()
            addExposedPort(CommonSettings.PipelineRunnerDefaultPort)
            withNetwork(vyneNetwork)
            withOption("$eurekaServerUri=$eurekaUri")
            if (alwaysPullImages) {
               withImagePullPolicy(PullPolicy.alwaysPull())
            }
         }


         return VyneSystem(eureka, vyneQueryServer, fileSchemaServer, cask, pipelineOrchestrator, pipelineRunnerApp, vyneNetwork)
      }


      /**
       * Creates a Vyne System consisting of Eureka, Vyne Query Server, File Schema Server and Cask docker images for the given docker image tag.
       * File schema server is configured to read the core schema from the provided git repository.
       * @param repoName Name of the taxonomy source repository.
       * @param branchName Name of the target repository branch
       * @param repoSshUri Git repo URI to clone via SSH
       * @param sshPrivateKeyPath Path (on your local host) to the private key that will be used clone the repo via SSH.
       * This key should be in RSA or in pem format (Formats supported by used Jsch library)
       * @param sshPassPhrase Pass Phrase for [sshPrivateKeyPath] (if required)
       * @param alwaysPullImages Flag to force pulling docker images. By default it is false so container images are always retrieved from local Docker Image cache
       */
      fun withEurekaAndGitBasedSchema(
         repoName: String,
         branchName: String,
         repoSshUri: String,
         sshPrivateKeyPath: String,
         tag: DockerImageTag = latest,
         alwaysPullImages: Boolean = false,
         sshPassPhrase: String? = null): VyneSystem {
         val vyneSystem = withEurekaAndFileBasedSchema(null, tag, alwaysPullImages)
         vyneSystem.fileSchemaServer.apply {
            withOption("--taxi.gitSchemaRepos[0].name=$repoName")
            withOption("--taxi.gitSchemaRepos[0].uri=$repoSshUri")
            withOption("--taxi.gitSchemaRepos[0].branch=$branchName")
            sshPassPhrase?.let {
               withOption("--taxi.gitSchemaRepos[0].sshPassPhrase=$it")
            }
            withCopyFileToContainer(MountableFile.forHostPath(sshPrivateKeyPath), "/tmp/id_rsa")
            withOption("--taxi.gitSchemaRepos[0].sshPrivateKeyPath=/tmp/id_rsa")
            withOption("--taxi.gitCloningJobEnabled=true")
         }
         return vyneSystem
      }
   }

   override fun close() {
      pipelineRunner.close()
      pipelineOrchestrator.close()
      caskServer.close()
      fileSchemaServer.close()
      vyneQueryServer.close()
      eurekaServer.close()
   }
}

interface VyneSystemVerifier {
   fun verify(vyneSystem: VyneSystem)
}
