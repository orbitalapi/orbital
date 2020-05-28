package io.vyne.schemas.taxi

import io.vyne.SchemaAggregator
import io.vyne.schemas.Schema


class TaxiSchemaAggregator : SchemaAggregator {
   override fun aggregate(schemas: List<Schema>): Pair<Schema?, List<Schema>> {
      val taxiSchemas = schemas.filterIsInstance(TaxiSchema::class.java)
      val remaining = schemas - taxiSchemas
      val aggregatedSchema = if (taxiSchemas.isNotEmpty()) {
         taxiSchemas.reduce { a, b -> a.merge(b) }
      } else null
      return aggregatedSchema to remaining
   }

}
