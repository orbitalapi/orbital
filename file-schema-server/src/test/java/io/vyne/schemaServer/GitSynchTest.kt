package io.vyne.schemaServer

import com.nhaarman.mockitokotlin2.*
import com.winterbe.expekt.should
import org.eclipse.jgit.api.CloneCommand
import org.junit.After
import org.junit.Test
import java.io.File

class GitSynchTest {
   private val mockCloneCommand = mock<CloneCommand>()

   @After
   fun cleanUp() {
      File("repoRoot").deleteRecursively()
   }

   @Test
   fun cloneReposTest() {
      // Given
      val gitConfigs = listOf(GitSchemaRepoConfig.GitRemoteRepo(
         name = "config1",
         branch = "branch1",
         sshPassPhrase = "sshPhasePhrase1",
         sshPrivateKeyPath = "keyPath1",
         uri = "uri1")
      )
      val gitSchemaRepoConfig = GitSchemaRepoConfig("repoRoot", gitConfigs)
      val gitSynch = GitSynch(gitSchemaRepoConfig)
      val gitSchemaRepoConfigsCaptor = argumentCaptor<File>()
      val uriArgumentCaptor = argumentCaptor<String>()
      val branchArgumentCaptor = argumentCaptor<String>()
      val transportCallbackCaptor = argumentCaptor<SshTransportConfigCallback>()

      whenever(mockCloneCommand.setDirectory(any())).thenReturn(mockCloneCommand)
      whenever(mockCloneCommand.setURI(any())).thenReturn(mockCloneCommand)
      whenever(mockCloneCommand.setBranch(any())).thenReturn(mockCloneCommand)
      whenever(mockCloneCommand.setTransportConfigCallback(any())).thenReturn(mockCloneCommand)

      // When
      gitSynch.cloneRepos(mockCloneCommand)

      // Then
      verify(mockCloneCommand, times(1)).setDirectory(gitSchemaRepoConfigsCaptor.capture())
      "repoRoot${File.separator}config1".should.equal(gitSchemaRepoConfigsCaptor.firstValue.path)
      verify(mockCloneCommand, times(1)).setURI(uriArgumentCaptor.capture())
      "uri1".should.equal(uriArgumentCaptor.firstValue)
      verify(mockCloneCommand, times(1)).setBranch(branchArgumentCaptor.capture())
      "branch1".should.equal(branchArgumentCaptor.firstValue)
      verify(mockCloneCommand, times(1)).setTransportConfigCallback(transportCallbackCaptor.capture())
      "keyPath1".should.equal(transportCallbackCaptor.firstValue.sshPrivateKeyPath)
      "sshPhasePhrase1".should.equal(transportCallbackCaptor.firstValue.sshPassPhrase)
      verify(mockCloneCommand, times(1)).call()
   }
}
