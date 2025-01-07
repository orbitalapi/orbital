package com.orbitalhq.schemaServer.core

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.WorkspaceFileProjectConfig
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.test.StepVerifier
import java.nio.file.Paths
import java.time.Duration

class ConfigTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `returns an empty config if config file doesn't exist`() {
      val empty = FileWorkspaceConfigLoader(
         Paths.get("/this/path/doesnt/exist"),
         eventDispatcher = mock { },
         projectManager = mock { })
         .load()
      // MP 06-Sep-24: We now populate the file with defaults,
      // as we need to make the paths relative to the config file.
//      empty.file.should.be.`null`
      empty.fileConfigOrDefault.projects.shouldBeEmpty()
      empty.file!!.projects.shouldBeEmpty()
      empty.git!!.repositories.shouldBeEmpty()
   }

   @Test
   fun `can read and write a full config`() {
      val config = WorkspaceConfig(
         file = WorkspaceFileProjectConfig(
            projects = listOf(
               FileProjectSpec(
                  path = Paths.get("/a/b/c/project"),
                  isEditable = true
               )
            ),
            newProjectsPath = Paths.get("/opt/var/orbital/projects")
         ),
         git = WorkspaceGitProjectConfig(
            checkoutRoot = Paths.get("/my/git/root"),
            repositories = listOf(
               GitProjectSpec(
                  "my-git-project",
                  "https://github.com/something.git",
                  branch = "master"
               )
            )
         ),

         )

      val path = folder.root.toPath().resolve("repo.conf")
      val configRepo = FileWorkspaceConfigLoader(
         path,
         eventDispatcher = mock { },
         projectManager = mock { })
      configRepo.save(config)
      val loaded = configRepo.load()
      loaded.should.equal(config)
   }


   @Test
   fun `file paths in config file are treated as relative if not explicitly absolute`() {
      val configFile = Resources.getResource("config-files/with-relative-path.conf")
         .toURI()
      val targetConfigFile = folder.newFile("workspace.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileWorkspaceConfigLoader(
         targetConfigFile.toPath(),
         eventDispatcher = mock { }, projectManager = mock { })
      val config = configRepo.load()
      config.file!!.projects.should.have.size(1)
      val path = config.file!!.projects[0].path

      path.should.equal(folder.root.resolve("path/to/project").toPath())
      config.file!!.newProjectsPath.shouldBe(folder.root.resolve("orbital/workspace/projects").toPath())
   }

   @Test
   fun `can read a file with schema projects configured`() {
      val configFile = Resources.getResource("config-files/with-package-project.conf")
         .toURI()
      val targetConfigFile = folder.newFile("server.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileWorkspaceConfigLoader(
         targetConfigFile.toPath(),
         eventDispatcher = mock { }, projectManager = mock { })
      val config = configRepo.load()

      config.file!!.projects.should.have.size(3)
      val loader = config.file!!.projects[1].loader as OpenApiPackageLoaderSpec
      loader.uri.toString().should.equal("http://acme.com/api/open-api")

      val soapSpec = config.file!!.projects[2].loader as SoapPackageLoaderSpec
      soapSpec.identifier.shouldBe(PackageIdentifier.fromId("com.acme/MySoap/0.1.20"))
      configRepo.save(config)
   }

   @Test
   fun `can configure a git package loader factory to use file system polling`() {
      val configFile = Resources.getResource("config-files/git-with-polling-file-watcher.conf")
         .toURI()
      val targetConfigFile = folder.newFile("server.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileWorkspaceConfigLoader(
         targetConfigFile.toPath(),
         eventDispatcher = mock { }, projectManager = mock { })
      val config = configRepo.load()

      config.git!!.diskChangeDetectionMethod.shouldBe(FileChangeDetectionMethod.POLL)
      config.git!!.diskPollFrequency.shouldBe(Duration.ofSeconds(120))

      val constructedLoader = GitSchemaPackageLoaderFactory()
         .build(config.git!!, config.git!!.repositories.single())

      val fileWatcher = constructedLoader.fileMonitor.shouldBeInstanceOf<ReactivePollingFileSystemMonitor>()
      fileWatcher.pollFrequency.shouldBe(Duration.ofSeconds(120))

   }

   @Test
   fun `syntax errors reported for an invalid workspace conf`() {
      val configFile = Resources.getResource("config-files/workspace-syntax-error.conf")
         .toURI()
      val targetConfigFile = folder.newFile("workspace-syntax-error.conf")
      IOUtils.copy(configFile.toURL().openStream(), targetConfigFile.outputStream())

      val configRepo = FileWorkspaceConfigLoader(
         targetConfigFile.toPath(),
         eventDispatcher = mock { }, projectManager = mock { })

      StepVerifier.create(configRepo.loaderStatus.take(1))
         .expectNextMatches {
            it.state == LoaderStatus.LoaderState.ERROR && it.message.contains("String: 1: Key 'this is an invalid workspace.conf' may not be followed by token: end of file")
         }
         .verifyComplete()

   }
}
