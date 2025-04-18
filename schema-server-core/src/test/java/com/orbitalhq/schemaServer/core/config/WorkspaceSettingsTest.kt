package com.orbitalhq.schemaServer.core.config

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.config.BaseHoconConfigFileRepository
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.GitWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.utils.asA
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import io.github.config4k.extract
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class WorkspaceSettingsTest {

   @TempDir
   lateinit var tempDir: Path

   @Test
   fun `when a workspace git url is provided then a git workspace loader is configured`() {
      WorkspaceSettings(
         git = WorkspaceGitSettings(
            url = URI.create("https://github.om/something.git").toURL(),
            branch = "main"
         )
      ).createLoader(mock { }, mock { }, workspaceConfig = mock {})
         .shouldBeInstanceOf<GitWorkspaceConfigLoader>()
   }

   @Test
   fun `workspace file is created if missing with default file monitoring settings`() {
      val workspaceFile = tempDir.resolve("workspace.conf")
      workspaceFile.shouldNotExist()
      val loader = WorkspaceSettings(
         configFile = workspaceFile
      )
         .createLoader(mock { }, mock { }, workspaceConfig = WorkspaceSettings())
      loader.shouldBeInstanceOf<FileWorkspaceConfigLoader>()
      (loader as FileWorkspaceConfigLoader)
      workspaceFile.shouldExist()

      val pollFrequency = Duration.ofSeconds(30)
      val workspaceConfig = SimpleWorkspaceConfigRepo(workspaceFile).typedConfig()
      workspaceConfig.file!!.changeDetectionMethod.shouldBe(FileChangeDetectionMethod.WATCH)
      workspaceConfig.file!!.pollFrequency.shouldBe(pollFrequency)
      workspaceConfig.git!!.diskChangeDetectionMethod.shouldBe(FileChangeDetectionMethod.WATCH)
      workspaceConfig.git!!.diskPollFrequency.shouldBe(pollFrequency)
   }

   @Test
   fun `workspace file is created if missing with povided file monitoring settings`() {
      val workspaceFile = tempDir.resolve("workspace.conf")
      workspaceFile.shouldNotExist()
      val pollFrequency = Duration.ofSeconds(60)
      val loader = WorkspaceSettings(
         configFile = workspaceFile
      )
         .createLoader(mock { }, mock { }, workspaceConfig = WorkspaceSettings(
            fileChangeDetectionMethod = FileChangeDetectionMethod.POLL,
            filePollFrequency = pollFrequency
         ))
      loader.shouldBeInstanceOf<FileWorkspaceConfigLoader>()
      workspaceFile.shouldExist()
      val workspaceConfig = SimpleWorkspaceConfigRepo(workspaceFile).typedConfig()
      workspaceConfig.file!!.changeDetectionMethod.shouldBe(FileChangeDetectionMethod.POLL)
      workspaceConfig.file!!.pollFrequency.shouldBe(pollFrequency)
      workspaceConfig.git!!.diskChangeDetectionMethod.shouldBe(FileChangeDetectionMethod.POLL)
      workspaceConfig.git!!.diskPollFrequency.shouldBe(pollFrequency)
   }

   @Test
   fun `if workspace file not present and a project path is provided then a file project is added to the workspace`() {
      val workspaceFile = tempDir.resolve("workspace.conf")
      val projectDir = tempDir.resolve("project").toFile()
      projectDir.mkdirs()
      projectDir.deployProject("sample-project")
      val taxiFile = projectDir.resolve("taxi.conf")
      taxiFile.shouldExist()
      workspaceFile.shouldNotExist()
      val loader = WorkspaceSettings(
         configFile = workspaceFile,
         projectFile = Paths.get("project/taxi.conf")
      )
         .createLoader(mock { }, mock { }, workspaceConfig = WorkspaceSettings())
      val loadedConfig = loader.load()

      val config = ConfigFactory.parseFile(workspaceFile.toFile())
      // Verify the path was written out relative to the workspace file,
      // not absolute
      val projectPathInConfigFile = config.getList("file.projects").first().asA<ConfigObject>().getValue("path").unwrapped()
      projectPathInConfigFile.shouldBe("project/taxi.conf")
   }


}

class SimpleWorkspaceConfigRepo(configFilePath: Path): BaseHoconConfigFileRepository<WorkspaceConfig>(configFilePath) {
   override fun extract(config: Config): WorkspaceConfig =  config.extract()

   override fun emptyConfig(): WorkspaceConfig = WorkspaceConfig(null, null)

}