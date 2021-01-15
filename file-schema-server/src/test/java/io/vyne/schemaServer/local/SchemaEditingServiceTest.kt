package io.vyne.schemaServer.local

import com.winterbe.expekt.should
import io.vyne.schemaServer.git.GitRepoProvider
import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.schemaServer.git.TestWithGitServer
import io.vyne.security.VyneUser
import org.junit.Before
import org.junit.Test

class SchemaEditingServiceTest : TestWithGitServer() {

   lateinit var service: SchemaEditingService
   lateinit var repoProvider: GitRepoProvider

   @Before
   override fun setup() {
      super.setup()
      repoProvider = GitRepoProvider(
         GitSchemaRepoConfig(
            folder.root.canonicalPath,
            listOf(
               gitRepoConfigFromServer(name = "TestRepo", editable = true)
            )
         )
      )

      service = SchemaEditingService(repoProvider)
   }

   @Test
   fun `can create a new file in default branch`() {
      service.writeContent(
         "TestRepo",
         "src/taxi/MyFile.taxi",
         "Hello World".toByteArray(),
         VyneUser.forUserId("martypitt")
      )

      // Now delete the directory and clone again
      resetCheckoutFolder()

      val clonedRepo = repoProvider.getRepository("TestRepo", cloneIfNotPresent = true)

      val checkedOutFile = clonedRepo.workingDir.resolve("src/taxi/MyFile.taxi").toFile()
      checkedOutFile.exists().should.be.`true`
      checkedOutFile.readText().should.equal("Hello World")
   }
}

