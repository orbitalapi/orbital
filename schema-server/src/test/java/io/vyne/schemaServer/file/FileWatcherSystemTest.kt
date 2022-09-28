package io.vyne.schemaServer.file

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.argumentCaptor
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
import io.vyne.schemaServer.core.file.*
import mu.KotlinLogging
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
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
      "vyne.schema-server.compileOnStartup=false"
   ]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class FileWatcherSystemTest {

   companion object {
      @ClassRule
      @JvmField
      val folder = TemporaryFolder()

      @BeforeClass
      @JvmStatic
      fun prepare() {
         folder.deployProject("sample-project")
         val createdFile = Files.createFile(folder.root.toPath().resolve("src/hello.taxi"))
         createdFile.toFile().writeText("Hello, world")
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisherTransport


   @Test
   fun `when taxi change detection method is watch starts the File Watcher`() {
      // when file is updated
      Thread.sleep(2000L)
      val fileToEdit = folder.root.toPath().resolve("src/hello.taxi").toFile()
      fileToEdit.writeText("Updated")
      KotlinLogging.logger {}.info { "Updated ${fileToEdit.canonicalPath}" }

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         argumentCaptor<List<SourcePackage>>().apply {
            verify(schemaPublisherMock).submitPackages(capture())
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
   @Import(SchemaPublicationConfig::class, FileWatcherBuilders::class, FileSystemMonitorLifecycleHandler::class)
   class TestConfig {
      @Bean
      fun configLoader(): SchemaRepositoryConfigLoader {
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               FileSystemSchemaRepositoryConfig(
                  changeDetectionMethod = FileChangeDetectionMethod.WATCH,
                  incrementVersionOnChange = false,
                  paths = listOf(folder.root.toPath())
               )
            )
         )
      }
   }
}
