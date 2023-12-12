package com.orbitalhq.schemaServer.core.git.packages

import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

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
      remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()
   }

}
