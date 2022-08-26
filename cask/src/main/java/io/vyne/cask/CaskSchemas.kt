package io.vyne.cask

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource

object CaskSchemas {
   val packageIdentifier: PackageIdentifier = PackageIdentifier("io.vyne.cask", "generated-schemas", "1.0.0")
   val packageMetadata = PackageMetadata.from(packageIdentifier)

   fun caskSourcePackage(sources:List<VersionedSource>):SourcePackage {
      return SourcePackage(packageMetadata, sources)
   }
   fun caskSourcePackage(source:VersionedSource):SourcePackage {
      return caskSourcePackage(listOf(source))
   }
}
