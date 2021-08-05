package io.vyne.schemaServer

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.schemaServer.git.GitSyncTask
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
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "taxi.gitCloningJobEnabled=true",
      "taxi.gitCloningJobPeriodMs=1000",
      "taxi.schema-recompile-interval-seconds=1",
   ]
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
   private var fileWatcher: FileWatcher? = null

   @Autowired
   private var filePoller: FilePoller? = null

   @Autowired
   private var gitSyncTask: GitSyncTask? = null

   @Test
   fun `when gitCloningJobEnabled is true starts the git cloner`() {
      filePoller.should.be.`null`

      // Not sure this should be asserted...
      fileWatcher.should.not.be.`null`
      gitSyncTask.should.not.be.`null`

      // expect initial state to be sent with default version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(listOf(
            VersionedSource(
               name = "local/hello.taxi",
               version = "0.1.0",
               content = "Hello, world"
            )
         ))
      }
      // when file is updated
      remoteRepoDir.root.toPath().resolve("hello.taxi").toFile().writeText("Updated")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "update" }.call()
      KotlinLogging.logger {}.info { "Updated hello.taxi" }

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(listOf(
            VersionedSource(
               name = "local/hello.taxi",
               version = "0.1.0",
               content = "Updated"
            )
         ))
      }
   }

   @Configuration
   @EnableAsync
   @EnableScheduling
   @EnableConfigurationProperties(GitSchemaRepoConfig::class)
   @ComponentScan(
      basePackageClasses = [CompilerService::class],
      excludeFilters = [ComponentScan.Filter(
         type = ASSIGNABLE_TYPE,
         classes = [SchemaServerApp::class]
      )]
   )
   class TestConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(applicationContext: ConfigurableApplicationContext) {
         TestPropertyValues.of(
            "taxi.schema-local-storage=" + localRepoDir.root,
            "taxi.git-schema-repos[0].name=local",
            "taxi.git-schema-repos[0].uri=" + remoteRepoDir.root.toURI(),
            "taxi.git-schema-repos[0].branch=master",
         ).applyTo(applicationContext)
      }
   }
}
