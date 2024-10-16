package com.orbitalhq.schemaServer.core.repositories

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecRemovedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.test.utils.FlakeyOnBuildServer
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.core.publisher.Flux
import reactor.kotlin.test.test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


class FileWorkspaceConfigLoaderTest {
   @field:TempDir
   lateinit var folder: File

   @field:TempDir
   lateinit var projectFolder: File

   lateinit var loader: FileWorkspaceConfigLoader
   lateinit var configFile: File
   lateinit var eventDispatcher: ProjectStoreLifecycleManager

   @BeforeEach
   fun setup() {
      configFile = folder.resolve("workspace.conf")
      configFile.createNewFile()
      eventDispatcher = ProjectStoreLifecycleManager()
      loader =
         FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock { })
   }

//   @Test
//   @FlakeyOnBuildServer
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
         FileProjectSpec(path = path)
      }
      val workspace = WorkspaceConfig(
         file = WorkspaceFileProjectConfig(
            projects = projects
         )
      )
      val hoconString = loader.getHoconString(workspace)
      configFile.writeText(hoconString)
      return hoconString
   }

   @Test
   fun `adding a new project with relative path is created relative to workspace file`() {
      loader.addFileSpec(
         FileProjectSpec(
            path = Paths.get("test-project"),
            packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
         )
      )
      val relativeTaxiFile = folder.resolve("test-project/taxi.conf")
      relativeTaxiFile.shouldExist()
   }

   @Test
   fun `adding a new project with relative path defining taxi conf path is created relative to workspace file`() {
      loader.addFileSpec(
         FileProjectSpec(
            path = Paths.get("test-project/taxi.conf"),
            packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
         )
      )
      val relativeTaxiFile = folder.resolve("test-project/taxi.conf")
      relativeTaxiFile.shouldExist()
   }

   @Test
   fun `can add existing project using relative file path`() {
      val projectHome = configFile.parentFile.resolve("test-project")
      projectHome.mkdirs()
      projectHome.deployProject("sample-project")

      // This is an existing project, so we don't pass the identifier
      val update = loader.addFileSpec(
         FileProjectSpec(
            path = Paths.get("test-project"),
         )
      )
      update.status.shouldBe(ModifyProjectResponseStatus.Ok)
   }

   @Test
   fun `can add existing project using absolute file path`() {
      val projectHome = configFile.parentFile.resolve("test-project")
      projectHome.mkdirs()
      projectHome.deployProject("sample-project")

      // This is an existing project, so we don't pass the identifier
      val update = loader.addFileSpec(
         FileProjectSpec(
            path = projectHome.toPath().toAbsolutePath()
         )
      )
      update.status.shouldBe(ModifyProjectResponseStatus.Ok)
   }

   @Test
   fun `adding a new project with absolute path is created in the correct location`() {
      loader.addFileSpec(
         FileProjectSpec(
            path = projectFolder.toPath().resolve("test-project"),
            packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
         )
      )
      val relativeTaxiFile = projectFolder.resolve("test-project/taxi.conf")
      relativeTaxiFile.shouldExist()
   }

   @Test
   fun `adding a new project with absolute path referencing taxi conf path is created in the correct location`() {
      loader.addFileSpec(
         FileProjectSpec(
            path = projectFolder.toPath().resolve("test-project/taxi.conf"),
            packageIdentifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
         )
      )
      val relativeTaxiFile = projectFolder.resolve("test-project/taxi.conf")
      relativeTaxiFile.shouldExist()
   }

   @Test
   fun `testing file path for relative path finds existing project relative to workspace file`() {
      val projectHome = folder.newFolder("sample-project")
      val deployedProject = projectHome.deployProject("sample-project")
      loader.validateProjectExists(FileProjectStoreTestRequest("sample-project"))
         .block()!!
         .exists.shouldBeTrue()
   }

   @Test
   fun `testing file path for absolute path finds existing project`() {
      val projectHome = folder.newFolder("sample-project")
      val deployedProject = projectHome.deployProject("sample-project")
      loader.validateProjectExists(FileProjectStoreTestRequest(projectHome.absolutePath))
         .block()!!
         .exists.shouldBeTrue()
   }

   @Test
   fun `testing file path for relative path correctly indicates no project present`() {
      loader.validateProjectExists(FileProjectStoreTestRequest("this-doesnt-exist"))
         .block()!!
         .exists.shouldBeFalse()
   }

   @Test
   fun `testing file path for absolute path correctly indicates no project present`() {
      loader.validateProjectExists(FileProjectStoreTestRequest(folder.resolve("this-doesnt-exist").absolutePath))
         .block()!!
         .exists.shouldBeFalse()
   }
}
