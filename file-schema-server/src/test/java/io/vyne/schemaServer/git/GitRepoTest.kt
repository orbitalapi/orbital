package io.vyne.schemaServer.git

import com.winterbe.expekt.should
import org.junit.Test

class GitRepoTest : TestWithGitServer() {


   @Test
   fun `will clone from remote repo`() {
      val repo = gitRepoFromServer()
      val result = repo.clone()
      result.should.equal(OperationResult.SUCCESS_WITH_CHANGES)
      folder.root.toPath().resolve("testfile").toFile().exists().should.be.`true`
   }

   @Test
   fun `can commit file changes in nested directory and push`() {

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
      resetCheckoutFolder()

      gitRepoFromServer().clone()

      folder.root.toPath().resolve("dummy.txt").toFile().exists().should.be.`true`
   }


}
