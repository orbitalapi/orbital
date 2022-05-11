package io.vyne.schema.spring

import io.vyne.VersionedSource
import io.vyne.asVersionedSource
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.loaders.FileSystemSourcesLoader
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.packages.TaxiSourcesLoader
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

class FileSchemaSourceProvider(private val resourcePath: Path) : InternalSchemaSourceProvider, SchemaProvider {
   override val versionedSources: List<VersionedSource> by lazy {
      TaxiSourcesLoader(resourcePath).load()
         .map { it.asVersionedSource() }
   }

   override val schema: Schema by lazy {
      TaxiSchema.from(this.versionedSources)
   }
}

interface InternalSchemaSourceProvider : SchemaSourceProvider

interface TaxiProjectSourceProvider {
   val versionedSources: List<VersionedSource>
}


/**
 * Loads VersionedSources from paths.
 *
 * The real work is deferred to SchemaSourcesLoader implementations,
 * which allow smart things like transpilation from Swagger / Protobuf,
 * loading Taxi projects, and introspecting zip files / jars.
 */
class ProjectPathSchemaSourceProvider(
   private val loaders: List<SchemaSourcesLoader>
) : InternalSchemaSourceProvider {
   constructor(loader: SchemaSourcesLoader) : this(listOf(loader))
   constructor(path: Path) : this(FileSystemSourcesLoader(path))
   constructor(url: URL) : this(Paths.get(url.toURI()))

   override val versionedSources: List<VersionedSource>
      get() {

         // Design choice:
         // Here, we used to do a bunch of spring-specific work.
         // For now, leave that to the caller.
         return loaders.flatMap { loader ->
            loader.load()
         }
      }
}


// Source is mutable for testing
class SimpleTaxiSchemaProvider(var source: String) : SchemaProvider {
   companion object {
      fun from(source: String): Pair<SimpleTaxiSchemaProvider, TaxiSchema> {
         val provider = SimpleTaxiSchemaProvider(source)
         return provider to provider.schema as TaxiSchema
      }
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
