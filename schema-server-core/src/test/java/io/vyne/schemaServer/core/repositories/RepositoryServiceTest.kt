package io.vyne.schemaServer.core.repositories

import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import io.vyne.schemaServer.repositories.CreateFileRepositoryRequest
import io.vyne.schemaServer.repositories.GitRepositoryChangeRequest
import io.vyne.spring.http.BadRequestException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith

class RepositoryServiceTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var repositoryService: RepositoryService

   @Before
   fun setup() {
      val configFile = folder.root.resolve("repositories.conf")
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())
      repositoryService = RepositoryService(loader)
   }

   @Test
   fun `can add a file repository`() {
      repositoryService.listRepositories()
         .file?.projects?.should?.be?.empty

      val folder = folder.newFolder("project")

      repositoryService.createFileRepository(
         CreateFileRepositoryRequest(
            folder.canonicalPath, true,
            PackageIdentifier.fromId("foo/test/0.1.0")
         )
      )

      val repositoryConfig = repositoryService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)

      val fileRepoPath = repositoryConfig.file!!.projects.single()
      fileRepoPath.path.toFile().canonicalPath.should.equal(folder.canonicalPath)
      fileRepoPath.isEditable.should.be.`true`
   }

   @Test
   fun `cannot add a duplicate file repository`() {
      val folder = folder.newFolder("project")

      val request = CreateFileRepositoryRequest(
         folder.canonicalPath,
         true,
         PackageIdentifier.fromId("foo/test/0.1.0")
      )
      repositoryService.createFileRepository(request)

      assertFailsWith<BadRequestException> {
         repositoryService.createFileRepository(request)
      }

   }

   @Test
   fun `cannot add a duplicate file repository with differing editable`() {
      val folder = folder.newFolder("project")

      val request = CreateFileRepositoryRequest(
         folder.canonicalPath, true,
         PackageIdentifier.fromId("foo/test/0.1.0")
      )
      repositoryService.createFileRepository(request)

      assertFailsWith<BadRequestException> {
         repositoryService.createFileRepository(request.copy(editable = false))
      }
   }

   @Test
   fun `creating a file repository where a taxi-conf doesnt exist then one is created`() {
      // use resolve, to ensure the repository creates the directory
      val folder = folder.root.resolve("project/")

      val request = CreateFileRepositoryRequest(
         folder.canonicalPath, true,
         PackageIdentifier.fromId("foo/test/0.1.0")
      )
      repositoryService.createFileRepository(request)

      folder.exists().should.be.`true`
      val taxiConfFile = folder.resolve("taxi.conf")
      taxiConfFile.exists().should.be.`true`
      taxiConfFile.isFile.should.be.`true`

      val sourceFolder = folder.resolve("src")
      sourceFolder.exists().should.be.`true`
      sourceFolder.isDirectory.should.be.`true`
   }

   @Test
   fun `can add a git repository`() {
      repositoryService.listRepositories()
         .git?.repositories?.should?.be?.empty

      repositoryService.createGitRepository(
         GitRepositoryChangeRequest(
            "test-repo",
            "https://github.com/test/repo",
            "master",
         )
      )

      repositoryService.listRepositories().git!!.repositories.should.have.size(1)
      val gitRepo = repositoryService.listRepositories().git!!.repositories.single()
      gitRepo.name.should.equal("test-repo")
      gitRepo.uri.should.equal("https://github.com/test/repo")
      gitRepo.branch.should.equal("master")
   }

}
