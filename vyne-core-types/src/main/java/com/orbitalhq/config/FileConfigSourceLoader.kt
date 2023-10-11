package com.orbitalhq.config

import com.typesafe.config.Config
import com.orbitalhq.*
import com.orbitalhq.utils.files.ReactiveFileSystemMonitor
import com.orbitalhq.utils.files.ReactiveWatchingFileSystemMonitor
import lang.taxi.packages.GlobPattern
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createFile
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class FileConfigSourceLoader(
   private val configFilePath: Path,
   private val fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(configFilePath),
   override val packageIdentifier: PackageIdentifier, /*= LOCAL_PACKAGE_IDENTIFIER, */
   /**
    * Optionally pass a glob pattern if configFilePath is a directory
    */
   private val glob: GlobPattern? = null,
   private val failIfNotFound: Boolean = true

) : ConfigSourceLoader, ConfigSourceWriter {

   companion object {
      private val logger = KotlinLogging.logger {}
      // For testing really.
      val LOCAL_PACKAGE_IDENTIFIER = PackageIdentifier(
         "local", "local", "0.1.0"
      )

      object CacheKey
   }

   private val sink = Sinks.many().multicast().directBestEffort<Class<out ConfigSourceLoader>>()
   private val contentCache = ConcurrentHashMap<CacheKey, List<SourcePackage>>()

   private val absolutePath = configFilePath.toFile().canonicalPath

   init {
      if (packageIdentifier == LOCAL_PACKAGE_IDENTIFIER) {
         logger.warn { "Loader for path $absolutePath was configured without a source package.  This can lead to sources being persisted to the wrong place." }
      }
      if (glob != null && configFilePath.isRegularFile()) {
         error("Glob patterns are only supported when the provided path is a directory")
      }
      fileMonitor.startWatching()
         .subscribe {
            logger.info { "Config file at $absolutePath has changed.  Invalidating cache, so will reload on next attempt" }
            contentCache.remove(CacheKey)
            sink.emitNext(FileConfigSourceLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
         }
   }

   override fun saveConfig(updated: Config) {
      val configWithPlaceholderQuotesRemoved = updated.getSafeConfigString()
      if (!Files.exists(configFilePath)) {
         configFilePath.createFile()
      } else {
         require(configFilePath.isRegularFile()) { "Expected the configured file path to be a file, but found a directory at $absolutePath" }
      }
      writeText(configFilePath, configWithPlaceholderQuotesRemoved)
   }

   override fun save(source: VersionedSource) {
      val path = if (glob == null) {
         require(source.name == configFilePath.toFile().name) { "This writer can only write to ${configFilePath.fileName}" }
         configFilePath
      } else {
         require(configFilePath.isDirectory()) { "When writing source with a glob pattern, the provided path is expected to be a directory" }
         configFilePath.resolve(source.name)
      }


      writeText(path, source.content)
   }

   private fun writeText(path: Path, text: String) {
      logger.info { "Saving updated config to $absolutePath and invalidating caches" }
      path.toFile().writeText(text)
      contentCache.remove(CacheKey)

      // Race condition prevention.
      // In theory, the file monitor should detect that the file has changed,
      // invalidate the cache and emit the signal.
      // However, it's more accurate to do it now.
      // Tests were failing without this.
      sink.emitNext(FileConfigSourceLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
   }

   override fun load(): List<SourcePackage> {
      return contentCache.getOrPut(CacheKey) {
         logger.info { "Reading file(s) at $configFilePath" }

         val sources = getSources(configFilePath)
         // Wrap the file in a source package.
         // This makes it easier to identify edits that are intended for this config file
         listOf(
            SourcePackage(
               PackageMetadata.from(packageIdentifier),
               sources,
               emptyMap()
            )
         )
      }
   }

   private fun getSources(configFilePath: Path): List<VersionedSource> {
      if (!Files.exists(configFilePath)) {
         if (failIfNotFound) {
            throw kotlin.io.NoSuchFileException(configFilePath.toFile())
         } else {
            logger.info { "No file found at $absolutePath.  Will ignore this source." }
            return emptyList()
         }
      }


      return if (configFilePath.isDirectory()) {
         val result =
            PathGlob(configFilePath, glob ?: "*.*").mapEachDirectoryEntry { path -> loadVersionedSource(path) }
               .values.toList()
         result
      } else {
         listOf(loadVersionedSource(configFilePath))
      }
   }

   private fun loadVersionedSource(path: Path): VersionedSource = VersionedSource(
      name = path.toString(),
      packageIdentifier.version,
      path.readText()
   )

   override val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() = sink.asFlux()
}
