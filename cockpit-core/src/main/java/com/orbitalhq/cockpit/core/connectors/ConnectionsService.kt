package com.orbitalhq.cockpit.core.connectors

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connections.ConnectionUsageMetadataRegistry
import com.orbitalhq.connectors.ConnectionDriverOptions
import com.orbitalhq.connectors.config.aws.AwsConnection
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnection
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.jdbc.JdbcDriverOptions
import com.orbitalhq.connectors.config.kafka.KafkaConnection
import com.orbitalhq.connectors.registry.ConnectionStatus
import com.orbitalhq.connectors.registry.ConnectorConfigDetail
import com.orbitalhq.connectors.registry.ConnectorConfiguration
import com.orbitalhq.connectors.registry.ConnectorConfigurationSummary
import com.orbitalhq.connectors.registry.ConnectorType
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.NotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@RestController
class ConnectionsService(
   private val connectorsRegistry: SourceLoaderConnectorsRegistry,
   private val schemaProvider: SchemaProvider,
   private val healthCheckService: ConnectionHealthCheckService
) {

   init {
      healthCheckService.getHealthCheckUpdates(Duration.ofSeconds(10))
         .subscribe { (config, status) ->
            configStatuses[config] = status
         }
   }

   private val configStatuses = ConcurrentHashMap<ConnectorConfiguration, ConnectionStatus>()

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/drivers")
   fun listAvailableDrivers(): Flux<ConnectionDriverOptions> {
      return Flux.fromIterable(JdbcDriverOptions.driverOptions + KafkaConnection.driverOptions + AwsConnection.driverOptions + AzureStorageConnection.driverOptions)
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections")
   fun listConnections(): Mono<ConnectionsListResponse> {
      val connections = this.connectorsRegistry.configSources
         .filter { !it.hasError }
         .flatMap { configSource ->
            configSource.typedConfig!!.listAll().map { connectorConfiguration ->
               val status = configStatuses.getOrDefault(connectorConfiguration, ConnectionStatus.unknown())
               ConnectorConfigurationSummary(configSource.packageIdentifier, connectorConfiguration, status)
            }
         }
      val errors = this.connectorsRegistry.configSources.filter { it.hasError }
         .map { configSource ->
            PackageWithError(configSource.packageIdentifier, configSource.error!!)
         }
      return Mono.just(ConnectionsListResponse(errors, connections))
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/{packageUri}/{connectionName}")
   fun getConnection(
      @PathVariable("packageUri") packageUri: String,
      @PathVariable("connectionName") connectionName: String
   ): Mono<ConnectorConfigDetail> {
      return listConnections().map { response ->
         val config =
            response.connections.singleOrNull { it.packageIdentifier.uriSafeId == packageUri && it.connectionName == connectionName }
               ?: throw NotFoundException("No connection was found for package $packageUri and name $connectionName")
         val usages = ConnectionUsageMetadataRegistry.findConnectionUsages(schemaProvider.schema, connectionName)
         ConnectorConfigDetail(
            config,
            usages
         )
      }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc")
   fun listDatabaseConnections(): Mono<List<ConnectorConfigurationSummary>> {
      return listConnections().map { connections ->
         connections.connections.filter { it.connectionType == ConnectorType.JDBC }
      }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/jdbc/{connectionName}")
   fun getConnection(@PathVariable("connectionName") connectionName: String): Mono<ConnectorConfigurationSummary> {
      return listDatabaseConnections().map { connections ->
         connections.singleOrNull { it.connectionName == connectionName }
            ?: throw NotFoundException("No connection named $connectionName was found")
      }
   }


}

@Deprecated("Use ConnectionStatus instead")
object ConnectionTestedSuccessfully {
   const val message: String = "ok"
}

data class ConnectionsListResponse(
   val definitionsWithErrors: List<PackageWithError>,
   val connections: List<ConnectorConfigurationSummary>
)

data class PackageWithError(
   val identifier: PackageIdentifier,
   val error: String
)
