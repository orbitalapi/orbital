package io.vyne.schemaServer.git

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import io.vyne.VersionedSource
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.SchemaServerApp
import io.vyne.schemaServer.openapi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.publisher.CompileOnStartupListener
import io.vyne.schemaStore.SchemaPublisher
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(
   webEnvironment = NONE,
)
@ContextConfiguration(
   initializers = [GitSyncTaskWithVersionIncrementContextTest.TestConfig::class]
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

         remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()

         val createdFile = Files.createFile(remoteRepoDir.root.toPath().resolve("hello.taxi"))

         createdFile.toFile().writeText("Hello, world")
         remoteRepo.add().addFilepattern(".").call()
         remoteRepo.commit().apply { message = "initial" }.call()
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisher

   @Autowired
   lateinit var gitSyncTask: GitSyncTask

   @Test
   fun `when gitCloningJobEnabled is true starts the git cloner`() {
      gitSyncTask.sync()

      // expect initial state to be sent with default version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(
            listOf(
               VersionedSource(
                  name = "hello.taxi",
                  version = "0.1.0",
                  content = "Hello, world"
               )
            )
         )
      }
      // when file is updated
      remoteRepoDir.root.toPath().resolve("hello.taxi").toFile().writeText("Updated")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "update" }.call()
      KotlinLogging.logger {}.info { "Updated hello.taxi" }

      gitSyncTask.sync()

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(
            listOf(
               VersionedSource(
                  name = "hello.taxi",
                  version = "0.1.0",
                  content = "Updated"
               )
            )
         )
      }
   }

   @Configuration
   @EnableConfigurationProperties(value = [GitSchemaRepositoryConfig::class, OpenApiSchemaRepositoryConfig::class])
   @ComponentScan(
      basePackageClasses = [CompilerService::class],
      excludeFilters = [ComponentScan.Filter(
         type = ASSIGNABLE_TYPE,
         classes = [SchemaServerApp::class,
            CompileOnStartupListener::class // This messes with our test assertions
         ]
      )]
   )
   class TestConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(applicationContext: ConfigurableApplicationContext) {
         TestPropertyValues.of(
            "vyne.schema-server.compileOnStartup=false",
            "vyne.schema-server.git.checkoutRoot=${localRepoDir.root}",
            "vyne.schema-server.git.repositories[0].name=local-test",
            "vyne.schema-server.git.repositories[0].uri=" + remoteRepoDir.root.toURI(),
            "vyne.schema-server.git.repositories[0].branch=master",
         ).applyTo(applicationContext)
      }
   }
}
