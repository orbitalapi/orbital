package com.orbitalhq.queryService

import com.orbitalhq.VersionedSource
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import com.orbitalhq.from
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema

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
