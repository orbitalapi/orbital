package io.vyne.schemaServer.git

import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class TestWithGitServer {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var gitServer: TestGitServer

   @Before
   open fun setup() {
      gitServer = TestGitServer.createStarted()
   }

   @After
   open fun tearDown() {
      gitServer.stop()
   }

   protected fun resetCheckoutFolder() {
      FileUtils.forceDelete(folder.root)
      FileUtils.forceMkdir(folder.root)
   }

   protected fun gitRepoFromServer(
      name: String = "TestRepo",
      branch: String = "master",
      editable: Boolean = false
   ): GitRepo {
      return GitRepo(
         folder.root.toPath(), gitRepoConfigFromServer(name, branch, editable)
      )
   }

   protected fun gitRepoConfigFromServer(
      name: String = "TestRepo",
      branch: String = "master",
      editable: Boolean = false
   ) = GitRepositoryConfig(
      name,
      gitServer.uri,
      branch,
      editable
   )


}
