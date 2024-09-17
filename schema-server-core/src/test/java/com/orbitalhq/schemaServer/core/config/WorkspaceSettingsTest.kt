package com.orbitalhq.schemaServer.core.config

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.FileWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.GitWorkspaceConfigLoader
import com.orbitalhq.utils.asA
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

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
      ).createLoader(mock { }, mock { })
         .shouldBeInstanceOf<GitWorkspaceConfigLoader>()
   }

   @Test
   fun `workspace file is created if missing`() {
      val workspaceFile = tempDir.resolve("workspace.conf")
      workspaceFile.shouldNotExist()
      val loader = WorkspaceSettings(
         configFile = workspaceFile
      )
         .createLoader(mock { }, mock { })
      loader.shouldBeInstanceOf<FileWorkspaceConfigLoader>()
      workspaceFile.shouldExist()
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
         .createLoader(mock { }, mock { })
      val loadedConfig = loader.load()

      val config = ConfigFactory.parseFile(workspaceFile.toFile())
      // Verify the path was written out relative to the workspace file,
      // not absolute
      val projectPathInConfigFile = config.getList("file.projects").first().asA<ConfigObject>().getValue("path").unwrapped()
      projectPathInConfigFile.shouldBe("project/taxi.conf")
   }


}
