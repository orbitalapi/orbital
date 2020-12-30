package io.vyne.schemaServer.git

import com.winterbe.expekt.should
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GitRepoTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var server: TestGitServer

   @Before
   fun setup() {
      server = TestGitServer.createStarted()
   }

   @After
   fun tearDown() {
      server.stop()
   }

   @Test
   fun `will clone from remote repo`() {
      val repo = gitRepoFromServer()
      val result = repo.clone()
      result.should.equal(OperationResult.SUCCESS_WITH_CHANGES)
      folder.root.toPath().resolve("testfile").toFile().exists().should.be.`true`
   }

   @Test
   fun `can commit file changes and push`() {
      val repo = gitRepoFromServer()
      repo.clone()
      val dummy = folder.newFile("dummy.txt")
      dummy.writeText("hello, world")

      repo.commitFile(dummy.toPath(), Author("Jimmy", "jimmy@foo.com"), "Test commit")
      val pushResult = repo.push()

      pushResult.should.equal(OperationResult.SUCCESS_WITHOUT_CHANGES)

      // Now delete and clone again to ensure it's on the remote
      FileUtils.forceDelete(folder.root)
      FileUtils.forceMkdir(folder.root)

      gitRepoFromServer().clone()

      folder.root.toPath().resolve("dummy.txt").toFile().exists().should.be.`true`
   }

   private fun gitRepoFromServer(): GitRepo {
      return GitRepo(folder.root.toPath(), GitRemoteRepository(
         "TestRepo",
         server.uri,
         "master"
      ))
   }
}
