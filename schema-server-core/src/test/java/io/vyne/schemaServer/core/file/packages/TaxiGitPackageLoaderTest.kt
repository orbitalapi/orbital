package io.vyne.schemaServer.core.file.packages

import io.vyne.schemaServer.core.file.deployProject
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder

class TaxiGitPackageLoaderTest {

   @ClassRule
   @JvmField
   val remoteRepoDir = TemporaryFolder()

   @ClassRule
   @JvmField
   val localRepoDir = TemporaryFolder()
   lateinit var remoteRepo: Git

   @Before
   fun createGitRemote() {

      remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()
      remoteRepoDir.deployProject("sample-project")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

}
