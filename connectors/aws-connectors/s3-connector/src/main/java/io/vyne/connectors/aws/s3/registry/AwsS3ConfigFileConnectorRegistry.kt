package io.vyne.connectors.aws.s3.registry

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.vyne.connectors.aws.s3.AwsS3ConnectionConnectorConfiguration
import io.vyne.connectors.registry.ConfigFileConnectorRegistry
import io.vyne.connectors.registry.ConnectionConfigMap
import java.nio.file.Path

class AwsS3ConfigFileConnectorRegistry(path: Path, fallback: Config = ConfigFactory.systemProperties()) :
   AwsS3ConnectionRegistry,
   ConfigFileConnectorRegistry<AWSS3Connections, AwsS3ConnectionConnectorConfiguration>(
      path,
      fallback,
      AWSS3Connections.CONFIG_PREFIX
   )  {

   override fun extract(config: Config): AWSS3Connections = config.extract()
   override fun emptyConfig(): AWSS3Connections = AWSS3Connections()
   override fun getConnectionMap(): Map<String, AwsS3ConnectionConnectorConfiguration> {
      return this.typedConfig().awsS3
   }

   override fun register(connectionConfiguration: AwsS3ConnectionConnectorConfiguration) {
      saveConnectorConfig(connectionConfiguration)
   }

   override fun listAll(): List<AwsS3ConnectionConnectorConfiguration> {
      return listConnections()
   }

   override fun remove(connectionConfiguration: AwsS3ConnectionConnectorConfiguration) {
      this.removeConnectorConfig(connectionConfiguration.connectionName)
   }

}


data class AWSS3Connections(
   val awsS3: MutableMap<String, AwsS3ConnectionConnectorConfiguration> = mutableMapOf()
) : ConnectionConfigMap {
   companion object {
      val CONFIG_PREFIX = AWSS3Connections::awsS3.name  // must match the name of the param in the constructor
   }
}
