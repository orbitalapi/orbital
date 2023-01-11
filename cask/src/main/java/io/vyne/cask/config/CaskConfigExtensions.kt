package io.vyne.cask.config

import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.cask.CaskSchemas
import io.vyne.cask.api.CaskConfig
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema


fun CaskConfig.schema(importSchema: Schema? = null): TaxiSchema {
   val sources = this.sourceSchemaIds.mapIndexed { index, schemaId ->
      val schemaSource = this.sources[index]
      VersionedSource.forIdAndContent(schemaId, schemaSource)
   }
   val imports = when (importSchema) {
      null -> emptyList()
      is TaxiSchema -> listOf(importSchema)
      is CompositeSchema -> (importSchema as CompositeSchema).taxiSchemas
      else -> error("Unhandled schema type")
   }
   return TaxiSchema.from(SourcePackage(CaskSchemas.packageMetadata, sources), imports)
}

fun CaskConfig.schema(import: TaxiSchema): TaxiSchema {
   val sources = this.sourceSchemaIds.mapIndexed { index, schemaId ->
      val schemaSource = this.sources[index]
      VersionedSource.forIdAndContent(schemaId, schemaSource)
   }
   return TaxiSchema.from(SourcePackage(CaskSchemas.packageMetadata, sources), listOf(import))
}
