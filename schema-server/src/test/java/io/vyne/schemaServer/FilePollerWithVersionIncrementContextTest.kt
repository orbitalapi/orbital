package io.vyne.schemaServer

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.schemaServer.git.GitSyncTask
import io.vyne.schemaServer.openapi.OpenApiServicesConfig
import io.vyne.schemaStore.SchemaPublisher
import mu.KotlinLogging
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
import java.util.concurrent.TimeUnit.SECONDS

@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "taxi.change-detection-method=poll",
      "taxi.schema-increment-version-on-recompile=true",
      "taxi.schema-poll-interval-seconds=1",
   ]
)
@ContextConfiguration(
   initializers = [FilePollerWithVersionIncrementContextTest.TestConfig::class]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class FilePollerWithVersionIncrementContextTest {

   companion object {
      @ClassRule
      @JvmField
      val folder = TemporaryFolder()

      @BeforeClass
      @JvmStatic
      fun prepare() {
         val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
         createdFile.toFile().writeText("Hello, world")
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
   fun `when taxi change detection method is poll starts the File Poller`() {
      fileWatcher.should.be.`null`
      gitSyncTask.should.be.`null`
      filePoller.should.not.be.`null`

      // expect initial state to be sent with default version
      verify(schemaPublisherMock).submitSchemas(listOf(VersionedSource(name="hello.taxi", version="0.1.0", content="Hello, world")))

      // when file is updated
      folder.root.toPath().resolve("hello.taxi").toFile().writeText("Updated")
      KotlinLogging.logger {}.info { "Updated hello.taxi" }

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(listOf(
            VersionedSource(
               name = "hello.taxi",
               version = "0.1.1",
               content = "Updated"
            )
         ))
      }
   }

   @Configuration
   @EnableAsync
   @EnableScheduling
   @EnableConfigurationProperties(value = [GitSchemaRepoConfig::class, OpenApiServicesConfig::class])
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
            "taxi.schema-local-storage=" + folder.root
         ).applyTo(applicationContext);
      }
   }
}
