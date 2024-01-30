package com.orbitalhq.config

import com.typesafe.config.Config
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import reactor.core.publisher.Flux

/**
 * Something which cna return one-or-more ConfigSourceWriter.
 *
 * Design choice: Using a provider as a wrapper around List<ConfigSourceWriter>
 * (versus just a list), so that things can vary the number of writers they return.
 * This is useful for things like SchemaSource (or ReactiveProjectStoreManager)
 * where the number of writer returned can vary based on their internal state.
 *
 * If you're here looking for implementations, look at ReactiveProjectStoreManager - that's the main one.
 *
 * Design choice: This same concept was originally applied to both readers and
 * writers. However, we already have SourceLoaderConnectorsRegistry,
 * which takes a collection ConfigSourceLoader.  Adding another abstraction
 * felt overkill.
 *
 * @see ProjectManagerConfigSourceLoader
 *
 */
interface ConfigSourceWriterProvider {
   fun getWriter(identifier: PackageIdentifier): ConfigSourceWriter
   fun hasWriter(identifier: PackageIdentifier): Boolean
}

// for testing
data class SimpleConfigSourceWriterProvider(
   val configSourceWriters: List<ConfigSourceWriter>
) : ConfigSourceWriterProvider {
   constructor(writer: ConfigSourceWriter) : this(listOf(writer))

   override fun getWriter(identifier: PackageIdentifier): ConfigSourceWriter {
      return this.configSourceWriters.first { it.packageIdentifier == identifier }
   }

   override fun hasWriter(identifier: PackageIdentifier): Boolean {
      return this.configSourceWriters.any { it.packageIdentifier == identifier }
   }
}

/**
 * A collection of all the ConfigSourceProviders.
 */
data class ConfigSourceRepository(
   // Note: This is not symmetrical - ConfigSourceLoader (returns one reader) and
   // ConfigSourceWriterProvider (returns many writers).
   // In time, we should move to LoaderProviders too - since the real underlying
   // implementation is the ReactiveProjectStoreManager, which can serve both
   // Loaders and Writers.
   private val loaders: List<ConfigSourceLoader>,
   private val writerProviders: List<ConfigSourceWriterProvider>
) : ConfigSourceWriterProvider {

   fun loadAll(): List<SourcePackage> {
      return this.loaders
         .flatMap { it.load() }
         .filter { it.sources.isNotEmpty() }
   }

   val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() {
         return this.loaders
            .map { it.contentUpdated }
            .let { fluxes -> Flux.merge(fluxes) }
      }

   override fun getWriter(identifier: PackageIdentifier): ConfigSourceWriter {
      this.writerProviders
         .firstOrNull { it.hasWriter(identifier) }
         ?.getWriter(identifier)
         ?.let {
            return it
         }

      return this.loaders
         .filterIsInstance<ConfigSourceWriter>()
         .firstOrNull { it.packageIdentifier == identifier }
         ?: error("No writer found for ${identifier.id}")
   }

   override fun hasWriter(identifier: PackageIdentifier): Boolean {
      val provider = this.writerProviders
         .firstOrNull { it.hasWriter(identifier) }
      return provider != null
   }
}


// This isn't really implemented.
// Pausing, as I think we just won't
// support ui-based writes for Auth tokens
// for a while
interface ConfigSourceWriter : ConfigSourceLoader {
   fun saveConfig(updated: Config)
   fun save(source: VersionedSource)

   val packageIdentifier: PackageIdentifier
}


fun List<ConfigSourceWriter>.getWriter(targetPackage: PackageIdentifier): ConfigSourceWriter {
   val writers = this.filter { it.packageIdentifier == targetPackage }
   return writers.singleOrNull()
      ?: error("Expected to find exactly 1 writer for package ${targetPackage.id}, but found ${writers.size}")
}

fun List<ConfigSourceWriter>.hasWriter(targetPackage: PackageIdentifier): Boolean {
   val writers = this.filter { it.packageIdentifier == targetPackage }
   return writers.size == 1
}

fun List<ConfigSourceLoader>.writers() = this.filterIsInstance<ConfigSourceWriter>()
