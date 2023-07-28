package io.vyne.schemaServer.core.repositories

import io.vyne.SourcePackage
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.repositories.lifecycle.NoOpRepositorySpecLifecycleEventDispatcher
import lang.taxi.utils.log
import mu.KotlinLogging
import java.nio.file.Path
import java.time.Duration

/**
 * Loads the sources and packages from the repositotires
 * defined in a config file.
 *
 * Intended for use in standalone environment where the config doesn't
 * change at runtime.
 *
 * If running in an environment where the config can change (ie.,
 * a UI app that supports adding / removing sources) then use a ReactiveRepositoryManager.
 */
class ConfigFileSourcesLoader(
   val configFilePath: Path,
   private val fileSystemPackageLoaderFactory: FileSystemPackageLoaderFactory = FileSystemPackageLoaderFactory(),
   private val gitPackageLoaderFactory: GitSchemaPackageLoaderFactory = GitSchemaPackageLoaderFactory()

) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val loadedPackages = buildSourceLoaderPublisher()
//   private val publisher:Mono<List<SourcePackage>> = buildSourceLoaderPublisher()

   private fun buildSourceLoaderPublisher(): List<SourcePackage> {
      val configLoader =
         FileSchemaRepositoryConfigLoader(configFilePath, eventDispatcher = NoOpRepositorySpecLifecycleEventDispatcher)
      val config = configLoader.load()

      val fileLoaders = config.file?.projects?.map { fileSpec ->
         fileSystemPackageLoaderFactory.build(config.file, fileSpec)
      } ?: emptyList()

      log().info("Building Git loaders")

      val gitLoaders = config.git?.repositories?.map { gitSpec ->
         gitPackageLoaderFactory.build(config.git, gitSpec)
      } ?: emptyList()

      log().info("Triggering load of source packages")

      val fileLoadedPackages = fileLoaders.map { it.loadNow().block(Duration.ofSeconds(30)) }
      val gitLoadedPackages = gitLoaders.map {
         it.start()
            .take(1).blockFirst(Duration.ofSeconds(30))
      }

      log().info("Load of source packages completed")

      val loaded = (fileLoadedPackages + gitLoadedPackages).filterNotNull()
      return loaded
//      return Flux.fromIterable(fileLoadedPackages + gitLoadedPackages)
//         .flatMap { it }
//         .collectList()
//         .cache()
   }

   fun load(): List<SourcePackage> {
      return loadedPackages
   }
}
