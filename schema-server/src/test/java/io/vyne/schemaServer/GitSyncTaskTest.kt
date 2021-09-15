package io.vyne.schemaServer

import com.nhaarman.mockitokotlin2.*
import com.winterbe.expekt.should
import io.vyne.schemaServer.git.GitRepo
import io.vyne.schemaServer.git.GitRepoProvider
import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.schemaServer.git.GitSyncTask
import org.junit.After
import org.junit.Test
import java.io.File

class GitSyncTaskTest {
   private val repoRoot = "${System.getProperty("user.dir")}${File.separator}repoRoot"
   private val mockGitRepoProvider = mock<GitRepoProvider>()
   private val mockGitRepo = mock<GitRepo>()
   private val mockFileWatcher = mock<FileWatcher>()
   private val mockVersionedSourceLoader = mock<VersionedSourceLoader>()
   private val mockCompilerService = mock<LocalFileSchemaPublisherBridge>()
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
   private val gitSynch = GitSyncTask(gitSchemaRepoConfig, mockGitRepoProvider, mockFileWatcher, mockVersionedSourceLoader, mockCompilerService)

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

      gitSynch.sync()

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

      gitSynch.sync()

      verify(mockGitRepoProvider, times(1)).provideRepo(repoRootCaptor.capture(), repoConfigCaptor.capture())
      repoRoot.should.equal(repoRootCaptor.firstValue)
      verify(mockGitRepo, times(1)).lsRemote()
      verify(mockGitRepo, times(1)).existsLocally()
      verify(mockGitRepo, times(1)).checkout()
      verify(mockGitRepo, times(1)).pull()
      verify(mockGitRepo, times(1)).close()
   }
}
