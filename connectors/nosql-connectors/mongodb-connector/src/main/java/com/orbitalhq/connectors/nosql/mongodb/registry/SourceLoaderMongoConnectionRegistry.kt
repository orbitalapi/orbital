package com.orbitalhq.connectors.nosql.mongodb.registry

import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.registry.MutableConnectionRegistry
import com.orbitalhq.connectors.registry.SourceLoaderConnectionRegistryAdapter

class SourceLoaderMongoConnectionRegistry(sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry) :
   MongoConnectionRegistry,
   MutableConnectionRegistry<MongoConnectionConfiguration>,
   SourceLoaderConnectionRegistryAdapter<MongoConnectionConfiguration>(
      sourceLoaderConnectorsRegistry,
      ConnectionsConfig::mongo
   )
