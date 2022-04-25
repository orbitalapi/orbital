package io.vyne.schema.api

import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema

/**
 * Exposes individual strings of schemas from somewhere (loaded from disk, generated from code).
 */
// REfactor 24-Apr-22: This used to be called SchemaSource, and there was a sepeprate class called
// SchemaSourceProvider.  Trying to simplify responsibilities
interface SchemaSourceProvider {
   val versionedSources: List<VersionedSource>

   val sourceContent: List<String>
      get() {
         return versionedSources.map { it.content }
      }

}


interface ParsedSourceProvider : SchemaSourceProvider {
   val parsedSources: List<ParsedSource>
}

/**
 * Responsible for exposing a Schema, based on multiple sources.
 *
 * There's tech debt here, as we used to think that we'd support multiple
 * independent schemas.
 *
 * However, we now handle combination during the Taxi compilation phase,
 * so there's only ever a single schema.  This idea has not been tidied up throughout
 * the code, so the List<Schema> vs Schema methods are still a mess.
 */
interface SchemaProvider : SchemaSourceProvider {
   val schema: Schema
      get() {
         return TaxiSchema.from(this.versionedSources)
      }

   /**
    * Returns a smaller schema only containing the requested members,
    * and their dependencies
    */
   @Deprecated("This is really complex, and I don't think it's used.  Can we remove it?")
   fun schema(memberNames: List<String>, includePrimitives: Boolean = false): Schema {
      val qualifiedNames = memberNames.map { it.fqn() }
      return MemberCollector(schema, includePrimitives).collect(qualifiedNames, mutableMapOf())
   }
}

/**
 * Combines the responsibilities of exposing indvidual taxi source code to the system,
 * along with providing a schema, compiled of multiple sources
 *
 * A SchemaStore will then hold the state of all the individual sources (published by SchemaSourceProviders)
 * and ultimately combining these into a Schema.
 *
 * Depending on configuration, individual services may have both a SchemaSourceProvider (to expose
 * sources), and a SchemaProvider (to compile and validate the sources).
 * However, it's equally valid to defer compilation
 */
//interface SchemaSourceProvider : SchemaProvider, SchemaSource {
//   override fun sources(): List<VersionedSource> {
//      if (this.schemas().any { it !is TaxiSchema }) {
//         // Use of non-taxi schemas is no longer supported, for the reasons outlined in
//         // SchemaAggregator.
//         // AS we move more aggressively towards type extensions, we need to simplify the
//         // schema support.
//         error("No longer supporting non TaxiSchema's.")
//      }
//      return schemas().map { it as TaxiSchema }.flatMap { it.sources }
//   }
//}


data class ControlSchemaPollEvent(val poll: Boolean)
