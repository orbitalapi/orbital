package com.orbitalhq.schemaServer.core.git.packages

import com.jayway.awaitility.Awaitility
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

abstract class BaseGitTest {

   lateinit var remoteRepo: Git
   @Rule
   @JvmField
   val configFolder = TemporaryFolder()

   @Rule
   @JvmField
   val remoteRepoDir = TemporaryFolder()

   @Rule
   @JvmField
   val localRepoDir = TemporaryFolder()


   @Before
   fun createGitRemote() {
      val repository = FileRepositoryBuilder.create(File(remoteRepoDir.root, ".git"))
      repository.create()
      remoteRepo = Git(repository)
   }

   protected fun deployTestProjectToRemoteGitPath(pathInRepository: Path = Paths.get("."), projectName: String = "sample-project") {
      remoteRepoDir.root.resolve(pathInRepository.toString()).toPath().deployProject(projectName)
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()


   }

   protected fun waitForSuccessfulGitClone(projectStoreManager: ReactiveProjectStoreManager, gitLoader: GitSchemaPackageLoader) {
      gitLoader.syncNow()
      Awaitility.await().atMost(com.jayway.awaitility.Duration.TEN_MINUTES)
         .until {
            // Observed that there's sometimes an underlying issue from JGit:
            // JGitInternalException - Creating directory /tmp/junit1281732176251018100/test-git-repo/.git/refs failed
            projectStoreManager.unhealthyLoaders.isEmpty()
         }

   }
}
