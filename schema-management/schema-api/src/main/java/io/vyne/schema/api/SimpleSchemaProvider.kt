package io.vyne.schema.api

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.Schema

// schema is mutable to allow reconfiguration during testing
class SimpleSchemaProvider(
   override var schema: Schema,
   var identifer: PackageIdentifier = PackageIdentifier("com.fake", "Fake", "0.0.0")
) : SchemaProvider {
   override val packages: List<SourcePackage>
      get() {
         return listOf(SourcePackage(PackageMetadata.from(identifer), versionedSources))
      }
   override val versionedSources: List<VersionedSource>
      get() {
         return schema.sources
      }

}
