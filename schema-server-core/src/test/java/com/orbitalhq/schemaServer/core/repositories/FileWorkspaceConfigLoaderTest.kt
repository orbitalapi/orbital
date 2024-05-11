package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Flux
import reactor.kotlin.test.test
import java.io.File
import java.nio.file.Path


class FileWorkspaceConfigLoaderTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var loader: FileWorkspaceConfigLoader
   lateinit var configFile: File
   lateinit var eventDispatcher: ProjectStoreLifecycleManager

   @Before
   fun setup() {
      configFile = folder.root.resolve("repositories.conf")
      eventDispatcher = ProjectStoreLifecycleManager()
      loader = FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
   }

   @Test
   fun `changes to file system that modifies path triggers remove and add`() {
      folder.deployProject("sample-project")
      allEvents()
         .test()
         .expectSubscription()
         .then {
            // Add a file spec. There's nothing deployed here, but the job of the workspace config
            // loader is simply to tell things that a project exists, not to verify it
            writeWorkspaceConfWithProjectPaths(
               folder.root.resolve("/wrongPath/taxi.conf").toPath()
            )
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecAddedEvent>()
            true
         }
         .then {
            writeWorkspaceConfWithProjectPaths(
               folder.root.resolve("/anotherPath/taxi.conf").toPath()
            )
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecRemovedEvent>()
               .spec.path.shouldBe(folder.root.resolve("/wrongPath/taxi.conf").toPath())
            true
         }
         .expectNextMatches { event ->
            event.shouldBeInstanceOf<FileSpecAddedEvent>()
               .spec.path.shouldBe(folder.root.resolve("/anotherPath/taxi.conf").toPath())
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
