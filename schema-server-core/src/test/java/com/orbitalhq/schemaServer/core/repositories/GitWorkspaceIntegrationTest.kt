package com.orbitalhq.schemaServer.core.repositories

import com.google.common.io.Resources
import com.orbitalhq.schemaServer.core.config.WorkspaceGitSettings
import com.orbitalhq.schemaServer.core.git.packages.BaseGitTest
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldStartWith
import lang.taxi.types.ObjectType
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.rules.TemporaryFolder
import reactor.core.publisher.Flux
import java.lang.reflect.Type
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.writeText

class GitWorkspaceIntegrationTest : BaseGitTest() {

   @Rule
   @JvmField
   val localGitFolder = TemporaryFolder()

   private fun deployWorkspaceFileToGitRepo(pathInRepository: Path = Paths.get("./workspace.conf")) {
      val workspaceConfig = Resources.getResource("config-files/full.conf").readText()
      Paths.get(remoteRepoDir.root.toURI()).resolve(pathInRepository).writeText(workspaceConfig)

      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

   @Test
   fun `can load a workspace from a git repo`() {
      val checkoutRoot = localGitFolder.newFolder("gitWorkspace")
      deployWorkspaceFileToGitRepo()
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader = GitWorkspaceConfigLoader(
         WorkspaceGitSettings(
            url = remoteRepoDir.root.toURL(),
            branch = "master",
            checkoutPath = checkoutRoot.toPath(),
            pollDuration = Duration.ofDays(1)
         ),
         eventDispatcher = eventDispatcher,
         syncUponInit = false // Only sync when we ask, otherwise there's race conditions in the tests
      )

      val config = loader.load()
      config.file?.projects?.shouldHaveSize(1)
      config.git?.repositories?.shouldHaveSize(1)
   }

   @Test
   fun `if workspace file not found then error emitted`() {
      val checkoutRoot = localGitFolder.newFolder("gitWorkspace")
      deployWorkspaceFileToGitRepo()
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader = GitWorkspaceConfigLoader(
         WorkspaceGitSettings(
            url = remoteRepoDir.root.toURL(),
            branch = "master",
            checkoutPath = checkoutRoot.toPath(),
            pollDuration = Duration.ofDays(1),
            path = Paths.get("doesntExist.conf")
         ),
         eventDispatcher = eventDispatcher,
         syncUponInit = false // Only sync when we ask, otherwise there's race conditions in the tests
      )

      val exception = assertThrows<IllegalStateException> {
         loader.load()
      }
      exception.message.shouldStartWith("No workspace file exists at")
   }
}
