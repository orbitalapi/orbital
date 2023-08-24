package io.vyne.connectors.kafka.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.connectors.config.kafka.KafkaConnectionConfiguration
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import java.nio.file.Path

class KafkaConfigFileConnectorRegistry(path: Path, fallback: Config = ConfigFactory.systemEnvironment()) :
   KafkaConnectionRegistry,
   ConfigFileConnectorRegistry<KafkaConnections, KafkaConnectionConfiguration>(
      path,
      fallback,
      KafkaConnections.CONFIG_PREFIX
   ) {

   override fun extract(config: Config): KafkaConnections = config.extract()
   override fun emptyConfig(): KafkaConnections = KafkaConnections()
   override fun getConnectionMap(): Map<String, KafkaConnectionConfiguration> {
      return this.typedConfig().kafka
   }

//   override fun register(connectionConfiguration: KafkaConnectionConfiguration) {
//      saveConnectorConfig(connectionConfiguration)
//   }

   override fun listAll(): List<KafkaConnectionConfiguration> {
      return listConnections()
   }

//   override fun remove(connectionConfiguration: KafkaConnectionConfiguration) {
//      this.removeConnectorConfig(connectionConfiguration.connectionName)
//   }


}

