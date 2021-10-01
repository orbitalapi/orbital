package io.vyne.connectors.kafka.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.connectors.kafka.DefaultKafkaConnectionConfiguration
import io.vyne.connectors.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import io.vyne.connectors.registry.ConnectionConfigMap
import java.nio.file.Path

class KafkaConfigFileConnectorRegistry(path: Path, fallback: Config = ConfigFactory.systemProperties()) :
   KafkaConnectionRegistry,
   ConfigFileConnectorRegistry<KafkaConnections, DefaultKafkaConnectionConfiguration>(
      path,
      fallback,
      KafkaConnections.CONFIG_PREFIX
   ) {

   override fun extract(config: Config): KafkaConnections = config.extract()
   override fun emptyConfig(): KafkaConnections = KafkaConnections()
   override fun getConnectionMap(): Map<String, DefaultKafkaConnectionConfiguration> {
      return this.typedConfig().kafka
   }

   override fun register(connectionConfiguration: KafkaConnectionConfiguration) {
      require(connectionConfiguration is DefaultKafkaConnectionConfiguration) { "Only DefaultJdbcConnectionConfiguration is supported by this class" }
      saveConnectorConfig(connectionConfiguration)
   }

   override fun listAll(): List<KafkaConnectionConfiguration> {
      return listConnections()
   }

   override fun remove(connectionConfiguration: KafkaConnectionConfiguration) {
      this.removeConnectorConfig(connectionConfiguration.connectionName)
   }


}

data class KafkaConnections(
   val kafka: MutableMap<String, DefaultKafkaConnectionConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = KafkaConnections::kafka.name  // must match the name of the param in the constructor
   }
}
