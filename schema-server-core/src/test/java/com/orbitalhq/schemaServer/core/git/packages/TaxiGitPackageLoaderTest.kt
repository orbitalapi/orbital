package com.orbitalhq.schemaServer.core.git.packages

import com.winterbe.expekt.should
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

class TaxiGitPackageLoaderTest {

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
   }

   private fun deployTestProjectToRemoteGitPath(pathInRepository: Path = Paths.get(".")) {
      remoteRepoDir.root.resolve(pathInRepository.toString()).toPath().deployProject("sample-project")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

   @Test
   fun `can load a taxi package from a git repo`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitRepositorySpec(
         "local-test",
         uri = remoteRepoDir.root.toURI().toASCIIString(),
         branch = "master",
         loader = TaxiPackageLoaderSpec
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
            e.sourcesWithPackageIdentifier.should.be.empty
            e.identifier.should.equal(
               PackageIdentifier(
                  organisation = "taxi",
                  name = "sample",
                  version = "0.3.0"
               )
            )
            true
         }
         .then {
            // commit some changes at the remote
            val file = remoteRepoDir.root.resolve("src/hello.taxi")
            file.writeText("type HelloWorld inherits String")
            remoteRepo.add().addFilepattern("src/hello.taxi").call()
            remoteRepo.commit().apply { message = "update" }.call()
            loader.syncNow()
            fileMonitor.pollNow()
         }
         .expectNextMatches { e ->
            e.sourcesWithPackageIdentifier.should.have.size(1)
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `can load a taxi package nested within a git repo`() {
      deployTestProjectToRemoteGitPath(pathInRepository = Paths.get("./taxonomy"))
      val file = remoteRepoDir.root.resolve("taxonomy/src/hello.taxi")
      file.writeText("type HelloWorld inherits String")
      remoteRepo.add().addFilepattern("taxonomy/src/hello.taxi").call()
      remoteRepo.commit().apply { message = "update" }.call()


      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitRepositorySpec(
         "local-test",
         uri = remoteRepoDir.root.toURI().toASCIIString(),
         branch = "master",
         loader = TaxiPackageLoaderSpec,
         path = Paths.get("taxonomy/")
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
                  organisation = "taxi",
                  name = "sample",
                  version = "0.3.0"
               )
            )
            true
         }
         .thenCancel()
         .verify()
   }


}
