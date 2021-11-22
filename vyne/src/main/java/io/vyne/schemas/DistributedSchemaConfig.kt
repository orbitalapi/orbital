package io.vyne.schemas

import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig

object DistributedSchemaConfig {
   const val SchemaCacheName = "vyneSchemaSet"
   fun vyneSchemaMapConfig(): MapConfig {
      return Config().getMapConfig(SchemaCacheName)
   }
}
