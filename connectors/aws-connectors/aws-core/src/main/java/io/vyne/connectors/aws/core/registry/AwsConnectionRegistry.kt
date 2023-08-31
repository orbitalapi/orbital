package io.vyne.connectors.aws.core.registry

import io.vyne.connectors.config.ConnectionsConfig
import io.vyne.connectors.config.SourceLoaderConnectorsRegistry
import io.vyne.connectors.config.aws.AwsConnectionConfiguration
import io.vyne.connectors.registry.ConnectionRegistry
import io.vyne.connectors.registry.SourceLoaderConnectionRegistryAdapter

interface AwsConnectionRegistry : ConnectionRegistry<AwsConnectionConfiguration>

class SourceLoaderAwsConnectionRegistry(
    sourceLoaderConnectorsRegistry: SourceLoaderConnectorsRegistry
) : AwsConnectionRegistry, SourceLoaderConnectionRegistryAdapter<AwsConnectionConfiguration>(
    sourceLoaderConnectorsRegistry,
   ConnectionsConfig::aws
)
