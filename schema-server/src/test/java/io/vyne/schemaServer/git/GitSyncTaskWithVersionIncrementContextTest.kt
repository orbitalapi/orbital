package io.vyne.schemaServer.git

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.asPackage
import io.vyne.schema.publisher.SchemaPublisherTransport
import io.vyne.schemaServer.SchemaPublicationConfig
import io.vyne.schemaServer.core.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.SchemaRepositoryConfig
import io.vyne.schemaServer.core.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.file.deployProject
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaConfiguration
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.git.GitSyncTask
import io.vyne.schemaServer.file.FilePollerSystemTest
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS

@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "vyne.schema-server.compileOnStartup=false",
   ]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class GitSyncTaskWithVersionIncrementContextTest {

   companion object {
      @ClassRule
      @JvmField
      val remoteRepoDir = TemporaryFolder()

      @ClassRule
      @JvmField
      val localRepoDir = TemporaryFolder()
      lateinit var remoteRepo: Git

      @BeforeClass
      @JvmStatic
      fun prepare() {
         remoteRepoDir.deployProject("sample-project")
         remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()

         val createdFile = Files.createFile(remoteRepoDir.root.toPath().resolve("src/hello.taxi"))

         createdFile.toFile().writeText("Hello, world")
         remoteRepo.add().addFilepattern(".").call()
         remoteRepo.commit().apply { message = "initial" }.call()
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisherTransport

   @Autowired
   lateinit var gitSyncTask: GitSyncTask


   @Test
   fun `when gitCloningJobEnabled is true starts the git cloner`() {
      gitSyncTask.sync()

      // expect initial state to be sent with default version
      await().atMost(Duration(15, SECONDS)).until {
         argumentCaptor<List<SourcePackage>>().apply {
            verify(schemaPublisherMock).submitPackages(capture())
            lastValue.single().sourcesWithPackageIdentifier.single().should.equal(
               VersionedSource(
                  name = "hello.taxi",
                  version = "0.3.0",
                  content = "Hello, world",
                  packageIdentifier = PackageIdentifier("taxi", "sample", "0.3.0")
               )
            )
         }
      }
      // when file is updated
      remoteRepoDir.root.toPath().resolve("src/hello.taxi").toFile().writeText("Updated")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "update" }.call()
      KotlinLogging.logger {}.info { "Updated hello.taxi" }

      gitSyncTask.sync()

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         argumentCaptor<List<SourcePackage>>().apply {
            verify(schemaPublisherMock, atLeastOnce()).submitPackages(capture())
            lastValue.single().sourcesWithPackageIdentifier.single().should.equal(
               VersionedSource(
                  name = "hello.taxi",
                  version = "0.3.0",
                  content = "Updated",
                  packageIdentifier = PackageIdentifier("taxi", "sample", "0.3.0")
               )
            )
         }
      }
   }

   @Profile("test")
   @Configuration
   @Import(SchemaPublicationConfig::class, GitSchemaConfiguration::class, GitSyncTask::class)
   class TestConfig {
      @Bean
      fun configLoader(): SchemaRepositoryConfigLoader {
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               git = GitSchemaRepositoryConfig(
                  checkoutRoot = localRepoDir.root.toPath(),
                  repositories = listOf(
                     GitRepositoryConfig(
                        "local-test",
                        uri = remoteRepoDir.root.toURI().toASCIIString(),
                        branch = "master"
                     )
                  )
               )
            )
         )
      }
   }
}
