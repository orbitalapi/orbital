package io.vyne.schemaServer.core.git.packages

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.adaptors.OpenApiPackageLoaderSpec
import io.vyne.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import io.vyne.schemaServer.core.file.packages.ReactivePollingFileSystemMonitor
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.core.git.GitSchemaPackageLoader
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.io.File
import java.time.Duration

class GitOpenApiPackageLoaderTest {
   @Rule
   @JvmField
   val remoteRepoDir = TemporaryFolder()

   @Rule
   @JvmField
   val localRepoDir = TemporaryFolder()
   lateinit var remoteRepo: Git

   @Before
   fun createGitRemote() {
      remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()
      val openApiSpec = File(Resources.getResource("open-api/petstore-expanded.yaml").toURI())
      FileUtils.copyFile(openApiSpec, remoteRepoDir.root.resolve("petstore-expanded.yaml"))
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

   @Test
   fun `can load an openApi package from a git repo`() {
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitRepositoryConfig(
         "local-test",
         uri = remoteRepoDir.root.toURI().toASCIIString(),
         branch = "master",
         loader = OpenApiPackageLoaderSpec(
            PackageIdentifier("com.acme", "petstore", "0"),
            defaultNamespace = "com.acme.pets"
         )
      )

      val adaptor = SchemaSourcesAdaptorFactory().getAdaptor(config.loader)
      val fileMonitor = ReactivePollingFileSystemMonitor(checkoutRoot, Duration.ofDays(1))
      val loader = GitSchemaPackageLoader(
         checkoutRoot,
         config,
         adaptor,
         fileMonitor,
         gitPollFrequency = Duration.ofDays(1)
      )

      loader.start()
         .test()
         .expectSubscription()
         .expectNextMatches { e ->
            e.sourcesWithPackageIdentifier.should.have.size(1)
            e.identifier.should.equal(
               PackageIdentifier(
                  organisation = "com.acme",
                  name = "petstore",
                  version = "1.0.0" // version is read from the OpenApi spec
               )
            )
            true
         }
         .thenCancel()
         .verify()
   }


}
