package com.orbitalhq.schemaServer.core.repositories

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.config.WorkspaceGitSettings
import com.orbitalhq.schemaServer.core.git.packages.BaseGitTest
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import reactor.test.StepVerifier
import java.net.URL
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

   @Before
   fun setup() {
      StepVerifier.setDefaultTimeout(Duration.ofSeconds(5))
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
         syncUponInit = false, // Only sync when we ask, otherwise there's race conditions in the tests
         projectManager = mock {  }
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
         , projectManager = mock {  }
      )

      val exception = assertThrows<IllegalStateException> {
         loader.load()
      }
      exception.message.shouldStartWith("No workspace file exists at")
   }

   @Test
   fun `emits healthy status when all ok`() {
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
         , projectManager = mock {  }
      )

      loader.loaderStatus
         .test()
         .expectSubscription()
         .expectNextMatches { it == LoaderStatus.STARTING }
         .then {
            loader.load()
         }
         .expectNextMatches {
            it == LoaderStatus.OK
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `emits unhealthy status when cannot clone workspace`() {
      val checkoutRoot = localGitFolder.newFolder("gitWorkspace")
      deployWorkspaceFileToGitRepo()
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader = GitWorkspaceConfigLoader(
         WorkspaceGitSettings(
            url = URL("https://gitlab.com/badurl.git"),
            branch = "master",
            checkoutPath = checkoutRoot.toPath(),
            pollDuration = Duration.ofDays(1)
         ),
         eventDispatcher = eventDispatcher,
         syncUponInit = false // Only sync when we ask, otherwise there's race conditions in the tests
         , projectManager = mock {  }
      )

      loader.loaderStatus
         .test()
         .expectSubscription()
         .expectNextMatches { it == LoaderStatus.STARTING }
         .then {
            try {
               loader.load()
            } catch (e:Exception) {}

         }
         .expectNextMatches {
            it.state.shouldBe(LoaderStatus.LoaderState.ERROR)
            true
         }
         .thenCancel()
         .verify()
   }
}
