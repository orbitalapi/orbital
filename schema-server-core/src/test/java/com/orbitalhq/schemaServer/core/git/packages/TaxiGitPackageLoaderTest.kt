package com.orbitalhq.schemaServer.core.git.packages

import com.winterbe.expekt.should
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import reactor.test.StepVerifier
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
      StepVerifier.setDefaultTimeout(Duration.ofSeconds(15))
   }

   private fun deployTestProjectToRemoteGitPath(pathInRepository: Path = Paths.get("."), update: Boolean = false) {
      remoteRepoDir.root.resolve(pathInRepository.toString()).toPath().deployProject("sample-project")
      remoteRepo.add().addFilepattern(".").setUpdate(update).call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

   @Test
   fun `can load a taxi package from a git repo`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectStoreSpec(
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
      val config = GitProjectStoreSpec(
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


   @Test
   fun `loader is healthy if everything syncs well`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectStoreSpec(
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

      loader.loaderStatus
         .test()
         .expectSubscription()
         .expectNextMatches { it == LoaderStatus.STARTING }
         .then {
            loader.syncNow()
            fileMonitor.pollNow()
         }
         .expectNextMatches {
            it.shouldBe(LoaderStatus.OK) // git status is OK

            true
         }
         // The internal file status should emit OK, which is filtered
         // as our loaded status is already OK, and we don't emit duplicates
         .expectNoEvent(Duration.ofSeconds(2))
         .thenCancel()
         .verify()
   }

   @Test
   fun `loader is unhealthy if bad git url`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectStoreSpec(
         "local-test",
         uri = "http://badgiturl.nope/", // <---- This is the test -- we can't sync from here, everythign should fail
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

      loader.loaderStatus
         .test()
         .expectSubscription()
         .expectNextMatches { it == LoaderStatus.STARTING }
         .then {
            loader.syncNow()
         }
         .expectNextMatches {
            it.state.shouldBe(LoaderStatus.LoaderState.ERROR)
            it.message.shouldBe("Failed to perform git sync to config local-test at http://badgiturl.nope/ - TransportException - http://badgiturl.nope/: cannot open git-upload-pack")
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `loader is unhealthy if taxi conf is missing`() {
      deployTestProjectToRemoteGitPath()
      val taxiConfFile = remoteRepoDir.root.resolve("taxi.conf")
      taxiConfFile.exists().shouldBe(true)
      taxiConfFile.delete()
      taxiConfFile.exists().shouldBe(false)
      remoteRepo.add().addFilepattern("taxi.conf").setUpdate(true).call()
      remoteRepo.commit().apply { message = "deleting taxi.conf" }.call()

      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectStoreSpec(
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

      loader.loaderStatus
         .test()
         .expectSubscription()
         .expectNextMatches { it == LoaderStatus.STARTING }
         .then {
            loader.syncNow()
            fileMonitor.pollNow()
         }
         .expectNextMatches {
            it.state.shouldBe(LoaderStatus.LoaderState.ERROR)
            it.message.shouldContain("No taxi.conf file found at ")
            true
         }
         // Now restore the file, to ensure the loader goes greent
         .then {
            deployTestProjectToRemoteGitPath(update = true)
            remoteRepo.add().addFilepattern("taxi.conf").call()
            remoteRepo.commit().apply { message = "restoring taxi.conf" }.call()
            loader.syncNow()
            fileMonitor.pollNow()
         }
         .expectNextMatches {
            it.shouldBe(LoaderStatus.OK)
            true
         }
         .thenCancel()
         .verify()
   }

}
