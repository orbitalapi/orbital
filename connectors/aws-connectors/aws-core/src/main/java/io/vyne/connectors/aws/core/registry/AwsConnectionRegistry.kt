package com.orbitalhq.connectors.aws.core.registry

import com.orbitalhq.connectors.config.ConnectionsConfig
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionRegistry
import com.orbitalhq.connectors.registry.SourceLoaderConnectionRegistryAdapter

interface AwsConnectionRegistry : ConnectionRegistry<AwsConnectionConfiguration>

class SourceLoaderAwsConnectionRegistry(
    sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry
) : AwsConnectionRegistry, SourceLoaderConnectionRegistryAdapter<AwsConnectionConfiguration>(
    sourceLoaderConnectorsRegistry,
   ConnectionsConfig::aws
)
