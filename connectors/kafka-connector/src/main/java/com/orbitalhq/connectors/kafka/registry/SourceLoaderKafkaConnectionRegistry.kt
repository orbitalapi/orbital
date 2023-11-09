package com.orbitalhq.connectors.kafka.registry

import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.kafka.KafkaConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.connectors.registry.SourceLoaderConnectionRegistryAdapter

class SourceLoaderKafkaConnectionRegistry(
   sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry
) : KafkaConnectionRegistry,
   MutableConnectionRegistry<KafkaConnectionConfiguration>,
   SourceLoaderConnectionRegistryAdapter<KafkaConnectionConfiguration>(
      sourceLoaderConnectorsRegistry,
      ConnectionsConfig::kafka
   )
