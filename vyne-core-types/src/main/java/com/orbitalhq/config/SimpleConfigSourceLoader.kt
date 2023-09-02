package com.orbitalhq.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import reactor.core.publisher.Flux

/**
 * Used for testing mainly
 */
class SimpleConfigSourceLoader(
   private val sources: List<VersionedSource>,
   packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("test/test/1.0.0")
) : ConfigSourceLoader {
   constructor(source: VersionedSource) : this(listOf(source))

   private val sourcePackage = SourcePackage(
      PackageMetadata.from(packageIdentifier),
      sources,
      emptyMap()
   )

   override fun load(): List<SourcePackage> {
      return listOf(sourcePackage)
   }

   override val contentUpdated: Flux<Class<out ConfigSourceLoader>> = Flux.never()
}
