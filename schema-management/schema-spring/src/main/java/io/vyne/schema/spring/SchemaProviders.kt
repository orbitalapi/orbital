package io.vyne.schema.spring

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.taxi.TaxiSchema


/**
 * Loads VersionedSources from paths.
 *
 * The real work is deferred to SchemaSourcesLoader implementations,
 * which allow smart things like transpilation from Swagger / Protobuf,
 * loading Taxi projects, and introspecting zip files / jars.
 *
 *
 * 10-Aug-22: Is this actually used?
 */
//class ProjectPathSchemaSourceProvider(
//   private val loaders: List<SchemaSourcesLoader>
//) : InternalSchemaSourceProvider {
//   constructor(loader: SchemaSourcesLoader) : this(listOf(loader))
//   constructor(path: Path) : this(FileSystemSourcesLoader(path))
//   constructor(url: URL) : this(Paths.get(url.toURI()))
//
//   override val versionedSources: List<VersionedSource>
//      get() {
//
//         // Design choice:
//         // Here, we used to do a bunch of spring-specific work.
//         // For now, leave that to the caller.
//         return loaders.flatMap { loader ->
//            loader.load()
//         }
//      }
//}
//

// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String, var identifer: PackageIdentifier = PackageIdentifier("com.fake", "Fake", "0.0.0")) : SchemaProvider {
   companion object {
      fun from(
         source: String,
         identifier: PackageIdentifier = PackageIdentifier("com.fake", "Fake", "0.0.0")
      ): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source, identifier)
         return provider to provider.schema as TaxiSchema
      }
   }

   override val packages: List<SourcePackage>
      get() {
         return listOf(SourcePackage(PackageMetadata.from(identifer), sources = versionedSources))
      }

   override val versionedSources: List<VersionedSource>
      get() {
         return listOf(VersionedSource.sourceOnly(source))
      }

   override val schema: TaxiSchema
      get() {
         return TaxiSchema.from(source)
      }

}
