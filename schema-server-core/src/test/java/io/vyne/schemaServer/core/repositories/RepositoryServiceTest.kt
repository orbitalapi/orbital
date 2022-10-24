package io.vyne.schemaServer.core.repositories

import com.winterbe.expekt.should
import io.vyne.schemaServer.core.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.repositories.FileRepositoryChangeRequest
import io.vyne.spring.http.BadRequestException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith
import kotlin.test.fail

class RepositoryServiceTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var repositoryService: RepositoryService

   @Before
   fun setup() {
      val configFile = folder.root.resolve("repositories.conf")
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath())
      repositoryService = RepositoryService(loader)
   }

   @Test
   fun `can add a file repository`() {
      repositoryService.listRepositories()
         .file?.projects?.should?.be?.empty

      val folder = folder.newFolder("project")

      repositoryService.createFileRepository(
         FileRepositoryChangeRequest(folder.canonicalPath, true)
      )

      val repositoryConfig = repositoryService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)

      val fileRepoConfig = repositoryConfig.file!!.projects.single()
      fileRepoConfig.path.toFile().canonicalPath.should.equal(folder.canonicalPath)
      fileRepoConfig.editable.should.be.`true`
   }

   @Test
   fun `cannot add a duplicate file repository`() {
      val folder = folder.newFolder("project")

      val request = FileRepositoryChangeRequest(folder.canonicalPath, true)
      repositoryService.createFileRepository(request)

      assertFailsWith<BadRequestException> {
         repositoryService.createFileRepository(request)
      }

   }

   @Test
   fun `cannot add a duplicate file repository with differing editable`() {
      val folder = folder.newFolder("project")

      val request = FileRepositoryChangeRequest(folder.canonicalPath, true)
      repositoryService.createFileRepository(request)

      assertFailsWith<BadRequestException> {
         repositoryService.createFileRepository(request.copy(editable = false))
      }
   }

   @Test
   fun `can add a git repository`() {

   }

}
