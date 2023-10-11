package com.orbitalhq.connectors.aws.core.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.registry.ConfigFileConnectorRegistry
import com.orbitalhq.connectors.registry.ConnectionConfigMap
import java.nio.file.Path

@Deprecated("Use SourceLoaderAwsConnectionRegistry")
class AwsConfigFileConnectionRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
   AwsConnectionRegistry, ConfigFileConnectorRegistry<AwsConnections, AwsConnectionConfiguration>(
   path,
   fallback,
   AwsConnections.CONFIG_PREFIX
) {
   override fun getConnectionMap(): Map<String, AwsConnectionConfiguration> {
      return this.typedConfig().aws
   }
   override fun listAll(): List<AwsConnectionConfiguration> {
      return listConnections()
   }

   override fun extract(config: Config): AwsConnections {
      return config.extract()
   }

   override fun emptyConfig(): AwsConnections {
      return AwsConnections()
   }
}

data class AwsConnections(
   val aws: MutableMap<String, AwsConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = AwsConnections::aws.name  // must match the name of the param in the constructor
   }
}
