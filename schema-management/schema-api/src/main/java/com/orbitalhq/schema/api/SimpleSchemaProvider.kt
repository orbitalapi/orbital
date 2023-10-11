package com.orbitalhq.schema.api

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schemas.Schema

// schema is mutable to allow reconfiguration during testing
class SimpleSchemaProvider(
   override var schema: Schema,
   var identifer: PackageIdentifier = PackageIdentifier("com.fake", "Fake", "0.0.0")
) : SchemaProvider {
   override val packages: List<SourcePackage>
      get() {
         return listOf(SourcePackage(PackageMetadata.from(identifer), versionedSources, emptyMap()))
      }
   override val versionedSources: List<VersionedSource>
      get() {
         return schema.sources
      }

}

class SchemaWithSourcesSchemaProvider(
   override val schema: Schema,
   override val packages: List<SourcePackage>
) : SchemaProvider {
   override val versionedSources: List<VersionedSource>
      get() {
         return packages.flatMap { it.sources }
      }

}

