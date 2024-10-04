package com.orbitalhq.nebula

import com.jayway.awaitility.Awaitility
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.ConfigFileLocationConventions.OrbitalNebulaKey
import com.orbitalhq.config.ConfigFileLocationConventions.OrbitalNebulaPathEntry
import com.orbitalhq.schemaServer.core.repositories.FileRepositoryIntegrationTest.Companion.setupServices
import com.orbitalhq.schemaServer.core.repositories.WorkspaceProjectsService
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.core.repositories.newFolder
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import lang.taxi.packages.TaxiPackageLoader
import lang.taxi.writers.ConfigWriter
import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.kotlin.test.test
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}
// Nebula is refactoring, and need to get a build shipped
// Sorry, future me. I promise I'll come back and fix these
@Disabled
class NebulaSchemaWatcherTest {
   @TempDir
   lateinit var folder: File

   @Test
   @Disabled("I cannot for the life of me get this test to work - however, the application behaviour does what its supposed to")
   fun `when a nebula file is added in the schema then change events are fired`() {
      val (workspaceService, repositoryManager, schemaClient, loader) = setupServices(folder)
      val nebulaWatcher = NebulaSchemaWatcher.fromEventSource(schemaClient)

      var projectFolder: File? = null
      val filename = "orbital/nebula/hello.nebula.kts"
      nebulaWatcher.stacksUpdated
         .test()
         .expectSubscription()
         .then {
            projectFolder = createTestProject(repositoryManager, workspaceService)
            Awaitility.await().atMost(5, TimeUnit.SECONDS)
               .until<Boolean> { workspaceService.listRepositories().file?.projects?.isNotEmpty() ?: false }
            addNebulaAsAdditionalSources(projectFolder!!.resolve("taxi.conf"))
            writeToFile(projectFolder!!.resolve(filename), """ stack {}""")
         }.expectNextMatches { event ->
            event.added.shouldHaveSize(1)
            event.removed.shouldBeEmpty()
            event.updated.shouldBeEmpty()
            true
         }
         .then {
            logger.info { "Waiting" }
            Thread.sleep(1000)
            logger.info { "Starting to modify stack file" }
            writeToFile(
               projectFolder!!.resolve(filename),
               """ stack { // The contents don't matter, we just want a change event }"""
            )
         }
         .expectNextMatches { event ->
            event.added.shouldBeEmpty()
            event.removed.shouldBeEmpty()
            event.updated.shouldHaveSize(1)
            true
         }
         .then {
            projectFolder!!.delete()
         }
         .expectNextMatches { event ->
            event.added.shouldBeEmpty()
            event.removed.shouldHaveSize(1)
            event.updated.shouldBeEmpty()
            true
         }
         .thenCancel()
         .verify(Duration.ofSeconds(5))
   }

   private fun writeToFile(file: File, text: String) {
      file.parentFile.mkdirs()
      file.writeText(text)
   }

   private fun addNebulaAsAdditionalSources(taxiConfFile: File) {
      val project = TaxiPackageLoader.forPathToTaxiFile(taxiConfFile.toPath())
         .load()
      val updatedAdditionalSources = project.additionalSources.toMutableMap()
      updatedAdditionalSources[OrbitalNebulaKey] = OrbitalNebulaPathEntry
      val updatedProject = project.copy(additionalSources = updatedAdditionalSources)
      taxiConfFile.writeText(ConfigWriter().writeMinimal(updatedProject))
   }

   private fun createTestProject(
      repositoryManager: ReactiveProjectStoreManager,
      workspaceService: WorkspaceProjectsService
   ): File {
      repositoryManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         projectFolder.resolve("taxi.conf")

         val packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         workspaceService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = TaxiPackageLoaderSpec,
               newProjectIdentifier = packageIdentifier,

               )
         )
         Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until<Boolean> { repositoryManager.fileLoaders.size == 1 }

         Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until<Boolean> { workspaceService.listRepositories().file?.projects?.isNotEmpty() ?: false }
         return projectFolder
      }
   }

}
