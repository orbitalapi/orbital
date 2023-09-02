package com.orbitalhq.schema.consumer

import com.orbitalhq.config.HoconConfigRepository
import lang.taxi.packages.SourcesType

/**
 * A Hocon respository that loads the config
 * from the schema, rather than disk
 */
abstract class BaseHoconSchemaRepository<T : Any>(
   schemaEventSource: SchemaChangedEventProvider,
   private val filename: String,
   private val sourceType: SourcesType = "@orbital/config"
) : HoconConfigRepository<T> {

}



