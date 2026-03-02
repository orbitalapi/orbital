package com.orbitalhq.schemaServer.core.git.packages

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.publisher.loaders.LoaderStatus
import com.orbitalhq.schemaServer.core.adaptors.SchemaSourcesAdaptorFactory
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitRepoSync
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoader
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
      val config = GitProjectSpec(
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
      val config = GitProjectSpec(
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
      val config = GitProjectSpec(
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
         .expectNextMatches {
            it == LoaderStatus.STARTING
         }
         .then {
            loader.syncNow()
            loader
               .filePackageLoader
               .start()
               .test()
               .expectSubscription()
               .expectNextMatches {
                  it.packageMetadata.identifier.id == "taxi/sample/0.3.0"
               }.thenCancel().verify()
         }
         .expectNextMatches {
            it.shouldBe(LoaderStatus.OK) // git status is OK

            true
         }
         .expectNoEvent(Duration.ofSeconds(2))
         .thenCancel()
         .verify()
   }

   @Test
   fun `loader is unhealthy if bad git url`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectSpec(
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
            it.message.shouldContain("Failed to sync git repository 'local-test' at http://badgiturl.nope/")
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `loader is warning (not error) if sync fails but repo was previously synced`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()

      // First, perform a successful sync to establish a local copy
      val goodConfig = GitProjectSpec(
         "local-test",
         uri = remoteRepoDir.root.toURI().toASCIIString(),
         branch = "master",
         loader = TaxiPackageLoaderSpec
      )
      GitRepoSync.syncNow(checkoutRoot, goodConfig)

      // Change the remote URL to a bad one so subsequent pulls actually fail.
      // pull() uses the git-configured remote from .git/config, not config.uri
      Git.open(checkoutRoot.toFile()).use { localGit ->
         val gitConfig = localGit.repository.config
         gitConfig.setString("remote", "origin", "url", "http://badgiturl.nope/")
         gitConfig.save()
      }

      // Now create a loader with a bad URL pointing to the same (already-synced) working dir
      val badConfig = GitProjectSpec(
         "local-test",
         uri = "http://badgiturl.nope/",
         branch = "master",
         loader = TaxiPackageLoaderSpec
      )
      val adaptor = SchemaSourcesAdaptorFactory().getAdaptor(badConfig.loader)
      val fileMonitor = ReactivePollingFileSystemMonitor(checkoutRoot, Duration.ofDays(1))
      val loader = GitSchemaPackageLoader(
         checkoutRoot,
         badConfig,
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
            it.state.shouldBe(LoaderStatus.LoaderState.WARNING)
            it.message.shouldContain("Remote sync failed — using locally cached version")
            it.message.shouldContain("Failed to sync git repository 'local-test'")
            true
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `source packages are still loaded from local copy when sync fails on a previously synced repo`() {
      deployTestProjectToRemoteGitPath()
      val checkoutRoot = localRepoDir.root.toPath()

      // First, perform a successful sync to establish a local copy
      val goodConfig = GitProjectSpec(
         "local-test",
         uri = remoteRepoDir.root.toURI().toASCIIString(),
         branch = "master",
         loader = TaxiPackageLoaderSpec
      )
      GitRepoSync.syncNow(checkoutRoot, goodConfig)

      // Change the remote URL to a bad one so subsequent pulls actually fail.
      // pull() uses the git-configured remote from .git/config, not config.uri
      Git.open(checkoutRoot.toFile()).use { localGit ->
         val gitConfig = localGit.repository.config
         gitConfig.setString("remote", "origin", "url", "http://badgiturl.nope/")
         gitConfig.save()
      }

      // Now create a loader with a bad URL pointing to the same (already-synced) working dir
      val badConfig = GitProjectSpec(
         "local-test",
         uri = "http://badgiturl.nope/",
         branch = "master",
         loader = TaxiPackageLoaderSpec
      )
      val adaptor = SchemaSourcesAdaptorFactory().getAdaptor(badConfig.loader)
      val fileMonitor = ReactivePollingFileSystemMonitor(checkoutRoot, Duration.ofDays(1))
      val loader = GitSchemaPackageLoader(
         checkoutRoot,
         badConfig,
         adaptor,
         fileMonitor,
         gitPollFrequency = Duration.ofDays(1)
      )

      // Even though the sync will fail, packages should still be emitted from the local copy
      loader.start()
         .test()
         .expectSubscription()
         .expectNextMatches { sourcePackage ->
            sourcePackage.identifier.shouldBe(
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
   fun `loader is unhealthy if taxi conf is missing`() {
      deployTestProjectToRemoteGitPath()
      val taxiConfFile = remoteRepoDir.root.resolve("taxi.conf")
      taxiConfFile.exists().shouldBe(true)
      taxiConfFile.delete()
      taxiConfFile.exists().shouldBe(false)
      remoteRepo.add().addFilepattern("taxi.conf").setUpdate(true).call()
      remoteRepo.commit().apply { message = "deleting taxi.conf" }.call()

      val checkoutRoot = localRepoDir.root.toPath()
      val config = GitProjectSpec(
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
            loader
               .filePackageLoader
               .start()
               .test()
               .expectSubscription()
               .expectNoEvent(Duration.ofSeconds(5)).thenCancel().verify()
         }
         .expectNextMatches {
            it.state.shouldBe(LoaderStatus.LoaderState.ERROR)
            it.message.shouldContain("No taxi.conf file found at ")
            true
         }
         // Now restore the file, to ensure the loader goes green
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
