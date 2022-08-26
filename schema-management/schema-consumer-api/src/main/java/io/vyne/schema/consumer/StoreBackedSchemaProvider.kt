package io.vyne.schema.consumer

import io.vyne.ParsedSource
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.ParsedSourceProvider
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Schema
import mu.KotlinLogging

/**
 * A Schema Provider which wraps a schema store.
 * This is the default way to expose a SchemaProvider, and generally
 * it's powered by a schema store that's consuming from a remote SchemaServer
 * (such as RSocketSchemaStore)
 */
class StoreBackedSchemaProvider(private val store: SchemaStore) : SchemaProvider, ParsedSourceProvider {
   private val logger = KotlinLogging.logger {}

   init {
      logger.info { "StoreBackedSchemaProvider started using a schema store of type ${store::class.java.simpleName}" }
   }

   override val parsedSources: List<ParsedSource>
      get() {
         return store.schemaSet.parsedPackages.flatMap { it.sources }
      }
   override val packages: List<SourcePackage>
      get() {
         return store.schemaSet.packages
      }

   override val schema: Schema
      get() {
         return store.schemaSet.schema
      }

   override val versionedSources: List<VersionedSource>
      get() {
         return store.schemaSet.allSources
      }
}


@Deprecated(replaceWith = ReplaceWith("StoreSchemaProvider"), message = "She's dead, Jim.")
object RemoteTaxiSourceProvider {
   // This class does nothing, but it's left here to improve
   // discoverability for developers looking for an old interface.
   // RemoteTaxiSourceProvider is now replaced by
   // StoreSchemaProvider
}
