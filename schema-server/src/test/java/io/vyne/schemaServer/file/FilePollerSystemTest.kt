package io.vyne.schemaServer.file

import com.jayway.awaitility.Awaitility.await
import com.jayway.awaitility.Duration
import com.nhaarman.mockito_kotlin.verify
import io.vyne.VersionedSource
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.SchemaServerApp
import io.vyne.schemaServer.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.openapi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaStore.SchemaPublisher
import mu.KotlinLogging
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
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
   initializers = [FilePollerSystemTest.TestConfig::class]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class FilePollerSystemTest {

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
      folder.root.toPath().resolve("hello.taxi").toFile().writeText("Updated")
      KotlinLogging.logger {}.info { "Updated hello.taxi" }

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
   @EnableConfigurationProperties(value = [GitSchemaRepositoryConfig::class, OpenApiSchemaRepositoryConfig::class, FileSystemSchemaRepositoryConfig::class])
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
            "vyne.schema-server.compileOnStartup=false",
            "vyne.schema-server.file.changeDetectionMethod=${FileChangeDetectionMethod.POLL}",
            "vyne.schema-server.file.incrementVersionOnChange=false",
            "vyne.schema-server.file.paths[0]=" + folder.root.canonicalPath,
         ).applyTo(applicationContext)
      }
   }
}
