package io.vyne.queryService

import io.vyne.VersionedSource
import io.vyne.cockpit.core.schemas.BuiltInTypesProvider
import io.vyne.from
import io.vyne.schema.api.SchemaProvider
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema

object TestSchemaProvider {
   fun withBuiltInsAnd(other: TaxiSchema): SchemaProvider {
      return withBuiltInsAnd(other.sources)
   }

   fun withBuiltInsAnd(sources: List<VersionedSource>): SchemaProvider {
      return SimpleSchemaProvider(
         TaxiSchema.from(
            BuiltInTypesProvider.sourcePackage.sources + sources
         )
      )
   }
}
