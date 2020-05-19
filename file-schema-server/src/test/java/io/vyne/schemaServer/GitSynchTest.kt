package io.vyne.schemaServer

import com.nhaarman.mockitokotlin2.*
import com.winterbe.expekt.should
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.After
import org.junit.Test
import java.io.File

class GitSynchTest {
   private val repoRoot = "${System.getProperty("user.dir")}${File.separator}repoRoot"
   private val mockGitRepoProvider = mock<GitRepoProvider>()
   private val mockGitRepo = mock<GitRepo>()
   private val gitConfigs = listOf(GitSchemaRepoConfig.GitRemoteRepo(
      name = "config1",
      branch = "branch1",
      sshPassPhrase = "sshPhasePhrase1",
      sshPrivateKeyPath = "keyPath1",
      uri = "uri1")
   )
   private val gitSchemaRepoConfig = GitSchemaRepoConfig(
      schemaLocalStorage = repoRoot,
      gitSchemaRepos = gitConfigs
   )
   private val gitSynch = GitSynch(gitSchemaRepoConfig, mockGitRepoProvider)

   private val repoRootCaptor = argumentCaptor<String>()
   private val repoConfigCaptor = argumentCaptor<GitSchemaRepoConfig.GitRemoteRepo>()

   @After
   fun cleanUp() {
      File(repoRoot).deleteRecursively()
   }

   @Test
   fun gitSynchCloneTest() {
      whenever(mockGitRepoProvider.provideRepo(any(), any())).thenReturn(mockGitRepo)
      whenever(mockGitRepo.existsLocally()).thenReturn(false)

      gitSynch.synch()

      verify(mockGitRepoProvider, times(1)).provideRepo(repoRootCaptor.capture(), repoConfigCaptor.capture())
      repoRoot.should.equal(repoRootCaptor.firstValue)
      verify(mockGitRepo, times(1)).lsRemote()
      verify(mockGitRepo, times(1)).existsLocally()
      verify(mockGitRepo, times(1)).checkout()
      verify(mockGitRepo, times(1)).clone()
      verify(mockGitRepo, times(1)).close()
   }

   @Test
   fun gitSynchPullTest() {
      whenever(mockGitRepoProvider.provideRepo(any(), any())).thenReturn(mockGitRepo)
      whenever(mockGitRepo.existsLocally()).thenReturn(true)

      gitSynch.synch()

      verify(mockGitRepoProvider, times(1)).provideRepo(repoRootCaptor.capture(), repoConfigCaptor.capture())
      repoRoot.should.equal(repoRootCaptor.firstValue)
      verify(mockGitRepo, times(1)).lsRemote()
      verify(mockGitRepo, times(1)).existsLocally()
      verify(mockGitRepo, times(1)).checkout()
      verify(mockGitRepo, times(1)).pull()
      verify(mockGitRepo, times(1)).close()
   }
}
