package io.vyne.testcontainers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.testcontainers.CommonSettings.actuatorHealthEndPoint
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.function.Predicate

object VyneContainerProvider {
   @JvmStatic
   val VyneQueryServerImage: DockerImageName = DockerImageName.parse("vyneco/vyne")

   @JvmStatic
   val CaskImage: DockerImageName = DockerImageName.parse("vyneco/cask")

   @JvmStatic
   val FileSchemaServer: DockerImageName = DockerImageName.parse("vyneco/file-schema-server")

   @JvmStatic
   val PipelineOrchestrator: DockerImageName = DockerImageName.parse("vyneco/pipelines-orchestrator")

   @JvmStatic
   val PipelineRunnerApp: DockerImageName = DockerImageName.parse("vyneco/pipeline-runner-app")

   @JvmStatic
   val Kafka: DockerImageName = DockerImageName.parse("lensesio/fast-data-dev")

   @JvmStatic
   val Eureka: DockerImageName = DockerImageName.parse("vyneco/eureka")
   fun vyneQueryServer(block: VyneContainer.() -> Unit = {}) = vyneQueryServer(CommonSettings.latest, block)
   fun vyneQueryServer(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val container = VyneContainer(VyneQueryServerImage.withTag(imageTag))
      container.setWaitStrategy(
         HttpWaitStrategy()
            .forPath(actuatorHealthEndPoint)
            .forStatusCode(200)
            .forResponsePredicate(ActuatorHealthStatusPredicate)
            .withStartupTimeout(Duration.ofMinutes(container.startUpTimeOutInMinutes)))
      block(container)
      return container
   }

   fun cask(block: VyneContainer.() -> Unit = {}) = cask(CommonSettings.latest, block)
   fun cask(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val caskServer = VyneContainer(CaskImage.withTag(imageTag))
      caskServer.setWaitStrategy(
         HttpWaitStrategy()
            .forPath(actuatorHealthEndPoint)
            .forStatusCode(200)
            .forResponsePredicate(ActuatorHealthStatusPredicate)
            .withStartupTimeout(Duration.ofMinutes(caskServer.startUpTimeOutInMinutes)))
      block(caskServer)
      return caskServer
   }

   fun fileSchemaServer(block: VyneContainer.() -> Unit = {}) = fileSchemaServer(CommonSettings.latest, block)
   fun fileSchemaServer(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val fileSchemaServer = VyneContainer(FileSchemaServer.withTag(imageTag))
      fileSchemaServer.setWaitStrategy(
         HttpWaitStrategy()
            .forPath(actuatorHealthEndPoint)
            .forStatusCode(200)
            .forResponsePredicate(ActuatorHealthStatusPredicate)
            .withStartupTimeout(Duration.ofMinutes(fileSchemaServer.startUpTimeOutInMinutes)))
      block(fileSchemaServer)
      return fileSchemaServer
   }

   fun eureka(block: VyneContainer.() -> Unit = {}) = eureka(CommonSettings.latest, block)
   fun eureka(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val eurekaContainer = VyneContainer(Eureka.withTag(imageTag))
      eurekaContainer.setWaitStrategy(
         HttpWaitStrategy()
            .forPath("/eureka/apps")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(eurekaContainer.startUpTimeOutInMinutes)))
      block(eurekaContainer)
      return eurekaContainer
   }

   fun pipelineOrchestrator(block: VyneContainer.() -> Unit = {}) = pipelineOrchestrator(CommonSettings.latest, block)
   fun pipelineOrchestrator(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val pipelineOrchestrator = VyneContainer(PipelineOrchestrator.withTag(imageTag))
      pipelineOrchestrator.setWaitStrategy(
         HttpWaitStrategy()
            .forPath(actuatorHealthEndPoint)
            .forStatusCode(200)
            .forResponsePredicate(ActuatorHealthStatusPredicate)
            .withStartupTimeout(Duration.ofMinutes(pipelineOrchestrator.startUpTimeOutInMinutes)))
      block(pipelineOrchestrator)
      return pipelineOrchestrator
   }

   fun pipelineRunnerApp(block: VyneContainer.() -> Unit = {}) = pipelineRunnerApp(CommonSettings.latest, block)
   fun pipelineRunnerApp(imageTag: DockerImageTag, block: VyneContainer.() -> Unit = {}): VyneContainer {
      val pipelineRunnerApp = VyneContainer(PipelineRunnerApp.withTag(imageTag))
      pipelineRunnerApp.setWaitStrategy(
         HttpWaitStrategy()
            .forPath(actuatorHealthEndPoint)
            .forStatusCode(200)
            .forResponsePredicate(ActuatorHealthStatusPredicate)
            .withStartupTimeout(Duration.ofMinutes(pipelineRunnerApp.startUpTimeOutInMinutes)))
      block(pipelineRunnerApp)
      return pipelineRunnerApp
   }
}

object ActuatorHealthStatusPredicate : Predicate<String> {
   private val objectMapper = jacksonObjectMapper()
   private val typeRef: TypeReference<Map<String, Any?>?> = object : TypeReference<Map<String, Any?>?>() {}
   private const val statusField = "status"
   private const val upValue = "UP"
   override fun test(response: String): Boolean {
      return try {
         val actuatorHealthResponse = objectMapper.readValue(response, typeRef)
         actuatorHealthResponse?.get(statusField) == upValue
      } catch (e: Exception) {
         false
      }
   }
}
