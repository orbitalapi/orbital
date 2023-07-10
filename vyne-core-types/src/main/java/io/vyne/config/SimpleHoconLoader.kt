package io.vyne.config

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import reactor.core.publisher.Flux

/**
 * Used for testing mainly
 */
class SimpleHoconLoader(
   private val sources: List<VersionedSource>,
   packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("test/test/1.0.0")
) : HoconLoader {
   constructor(source: VersionedSource) : this(listOf(source))

   private val sourcePackage = SourcePackage(
      PackageMetadata.from(packageIdentifier),
      sources,
      emptyMap()
   )

   override fun load(): List<SourcePackage> {
      return listOf(sourcePackage)
   }

   override val contentUpdated: Flux<Class<out HoconLoader>> = Flux.never()
}
