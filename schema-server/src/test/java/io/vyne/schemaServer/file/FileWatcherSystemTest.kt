package io.vyne.schemaServer.file

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import io.vyne.VersionedSource
import io.vyne.schema.publisher.SchemaPublisher
import io.vyne.schemaServer.SchemaPublicationConfig
import io.vyne.schemaServer.core.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.SchemaRepositoryConfig
import io.vyne.schemaServer.core.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.file.FileChangeDetectionMethod
import io.vyne.schemaServer.core.file.FileSystemMonitorLifecycleHandler
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.file.FileWatcherBuilders
import mu.KotlinLogging
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
         val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
         createdFile.toFile().writeText("Hello, world")
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisher


   @Test
   fun `when taxi change detection method is watch starts the File Watcher`() {
      // when file is updated
      Thread.sleep(2000L)
      val fileToEdit = folder.root.toPath().resolve("hello.taxi").toFile()
      fileToEdit.writeText("Updated")
      KotlinLogging.logger {}.info { "Updated ${fileToEdit.canonicalPath}" }

      // then updated state is sent with same version
      await().atMost(Duration(15, SECONDS)).until {
         verify(schemaPublisherMock).submitSchemas(listOf(
            VersionedSource(
               name = "hello.taxi",
               version = "0.1.0",
               content = "Updated"
            )
         ))
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
