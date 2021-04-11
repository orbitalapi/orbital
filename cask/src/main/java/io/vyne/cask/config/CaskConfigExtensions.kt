package io.vyne.cask.config

import io.vyne.VersionedSource
import io.vyne.cask.api.CaskConfig
import io.vyne.schemas.CompositeSchema
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument

fun CaskConfig.schema(importSchema:Schema? = null):TaxiSchema {
   val sources = this.sourceSchemaIds.mapIndexed { index, schemaId ->
      val schemaSource = this.sources[index]
      VersionedSource.forIdAndContent(schemaId,schemaSource)
   }
   val imports = when (importSchema) {
      null -> emptyList<TaxiSchema>()
      is TaxiSchema -> listOf(importSchema)
      is CompositeSchema -> (importSchema as CompositeSchema).taxiSchemas
      else -> error("Unhandled schema type")
   }
   return TaxiSchema.from(sources, imports)
}

fun CaskConfig.schema(import: TaxiSchema): TaxiSchema {
   val sources = this.sourceSchemaIds.mapIndexed { index, schemaId ->
      val schemaSource = this.sources[index]
      VersionedSource.forIdAndContent(schemaId,schemaSource)
   }
   return TaxiSchema.from(sources, listOf(import))
}
