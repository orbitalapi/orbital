package com.orbitalhq.schemaServer.core.repositories

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.test.utils.FlakeyOnBuildServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.core.publisher.Flux
import reactor.kotlin.test.test
import java.io.File
import java.nio.file.Path


class FileWorkspaceConfigLoaderTest {
   @field:TempDir
   lateinit var folder :File

   lateinit var loader: FileWorkspaceConfigLoader
   lateinit var configFile: File
   lateinit var eventDispatcher: ProjectStoreLifecycleManager

   @BeforeEach
   fun setup() {
      configFile = folder.resolve("repositories.conf")
      eventDispatcher = ProjectStoreLifecycleManager()
      loader = FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock {  })
   }

   @Test
   @FlakeyOnBuildServer
   fun `changes to file system that modifies path triggers remove and add`() {
      folder.deployProject("sample-project")
      allEvents()
         .test()
         .expectSubscription()
         .then {
            // Add a file spec. There's nothing deployed here, but the job of the workspace config
            // loader is simply to tell things that a project exists, not to verify it
            writeWorkspaceConfWithProjectPaths(
               folder.resolve("/wrongPath/taxi.conf").toPath()
            )
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecAddedEvent>()
            true
         }
         .then {
            writeWorkspaceConfWithProjectPaths(
               folder.resolve("/anotherPath/taxi.conf").toPath()
            )
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecRemovedEvent>()
               .spec.path.shouldBe(folder.resolve("/wrongPath/taxi.conf").toPath())
            true
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecAddedEvent>()
               .spec.path.shouldBe(folder.resolve("/anotherPath/taxi.conf").toPath())
            true
         }
         .thenCancel()
         .verify()
   }

   private fun allEvents() = Flux.merge(
      eventDispatcher.fileSpecRemoved,
      eventDispatcher.fileSpecAdded,
      eventDispatcher.gitSpecRemoved,
      eventDispatcher.gitSpecAdded
   )

   private fun writeWorkspaceConfWithProjectPaths(vararg paths: Path): String {
      val projects = paths.map { path ->
         FileSystemPackageSpec(path = path)
      }
      val workspace = WorkspaceConfig(
         file = FileSystemSchemaRepositoryConfig(
            projects = projects
         )
      )
      val hoconString = loader.getHoconString(workspace)
      configFile.writeText(hoconString)
      return hoconString
   }

}
