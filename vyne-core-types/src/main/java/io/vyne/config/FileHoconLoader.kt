package io.vyne.config

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.utils.files.ReactiveFileSystemMonitor
import io.vyne.utils.files.ReactiveWatchingFileSystemMonitor
import mu.KotlinLogging
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.readText

class FileHoconLoader(
   private val configFilePath: Path,
   private val fileMonitor: ReactiveFileSystemMonitor = ReactiveWatchingFileSystemMonitor(configFilePath),
   private val packageIdentifier: PackageIdentifier = DUMMY_PACKAGE_IDENTIFIER

) : HoconLoader {

   companion object {
      private val logger = KotlinLogging.logger {}
      val DUMMY_PACKAGE_IDENTIFIER = PackageIdentifier(
         "local", "local", "0.1.0"
      )

      object CacheKey
   }

   private val sink = Sinks.many().multicast().directBestEffort<Class<out HoconLoader>>()
   private val contentCache = ConcurrentHashMap<CacheKey, List<SourcePackage>>()

   init {
      fileMonitor.startWatching()
         .subscribe {
            logger.info { "Config file at $configFilePath has changed.  Invalidating cache, so will reload on next attempt" }
            contentCache.remove(CacheKey)
            sink.emitNext(FileHoconLoader::class.java, Sinks.EmitFailureHandler.FAIL_FAST)
         }
   }

   override fun load(): List<SourcePackage> {
      return contentCache.getOrPut(CacheKey) {
         logger.info { "Reading config file at $configFilePath" }
         // Wrap the file in a source package.
         // This makes it easier to identify edits that are intended for this config file
         listOf(
            SourcePackage(
               PackageMetadata.from(packageIdentifier),
               listOf(
                  VersionedSource(
                     name = configFilePath.toString(),
                     packageIdentifier.version,
                     configFilePath.readText()
                  )
               )
            )
         )
      }
   }

   override val contentUpdated: Flux<Class<out HoconLoader>>
      get() = sink.asFlux()
}
