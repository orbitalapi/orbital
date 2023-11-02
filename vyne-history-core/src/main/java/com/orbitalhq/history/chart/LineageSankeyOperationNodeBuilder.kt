package com.orbitalhq.history.chart

import com.orbitalhq.models.OperationResult
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.connectors.CacheConnectionName
import com.orbitalhq.query.connectors.CacheNames
import com.orbitalhq.query.history.CacheNode
import com.orbitalhq.query.history.DatabaseNode
import com.orbitalhq.query.history.HttpOperationNode
import com.orbitalhq.query.history.KafkaOperationNode
import com.orbitalhq.query.history.SankeyOperationNodeDetails
import com.orbitalhq.schemas.*
import lang.taxi.annotations.HttpOperation
import mu.KotlinLogging

/**
 * Builds OperationNodeDetails by looking up an OperationResult's Operation from the schema,
 * then generating operation specific metadata
 */
class LineageSankeyOperationNodeBuilder(private val schema: Schema) {
   private val logger = KotlinLogging.logger {}
   private val jdbcTableAnnotationName = "com.orbitalhq.jdbc.Table".fqn()

   fun buildOperationNode(operationResult: OperationResult): SankeyOperationNodeDetails? {
      if (isCachedOperation(operationResult.remoteCall.operationQualifiedName)) {
         return buildCacheHit(operationResult)
      }
      val (service, operation) = schema.remoteOperation(operationResult.remoteCall.operationQualifiedName)
      return when {
         isHttpApi(service, operation) -> buildHttpApi(service, operation, operationResult)
         isKafkaTopic(service, operation) -> buildKafkaTopicNode(service, operation, operationResult)
         isDatabaseQuery(service, operation) -> buildDatabaseNode(service, operation, operationResult)
         else -> {
            logger.debug { "No SankeyOperationNodeDetails building strategy found for Service ${service.name.shortDisplayName}. Consider adding one" }
            null
         }
      }
   }

   private fun buildCacheHit(operationResult: OperationResult):SankeyOperationNodeDetails {
      val exchange = operationResult.remoteCall.exchange as CacheExchange
      return CacheNode(
         connectionName = exchange.connectionName,
         cacheName = exchange.cacheName,
         cacheKey = exchange.cacheKey,
      )
   }

   private fun isCachedOperation(operationQualifiedName: QualifiedName): Boolean {
      val (service, operation) = OperationNames.serviceAndOperation(operationQualifiedName)
      return CacheNames.isCacheName(service)

   }

   private fun buildDatabaseNode(
      service: Service,
      operation: RemoteOperation,
      operationResult: OperationResult
   ): SankeyOperationNodeDetails? {
      val connectionName =
         service.metadata("com.orbitalhq.jdbc.DatabaseService")?.params?.get("connection") as? String?
            ?: "Unknown Db Connection"
      val memberType = operation.returnType.collectionType ?: operation.returnType

      // TODO : This is going to be complex once support for Join types is merged.
      // The return type could be a union type, and the return type indicates what could possibly retuned,
      // but isn't the same as being the tables that were selected.

      val tableNames = if (memberType.hasMetadata(jdbcTableAnnotationName)) {
         listOf(memberType.getMetadata(jdbcTableAnnotationName).params["table"] as String)
      } else {
         emptyList()
      }
      return DatabaseNode(
         connectionName,
         tableNames = tableNames
      )
   }

   private fun isDatabaseQuery(service: Service, operation: RemoteOperation): Boolean {
      // Note: Not using static constants here, as don't want a compile time dependency between the JDBC Connector
      // and History Core.
      return service.hasMetadata("com.orbitalhq.jdbc.DatabaseService") && operation is QueryOperation
   }

   private fun buildKafkaTopicNode(
      service: Service,
      operation: RemoteOperation,
      operationResult: OperationResult
   ): SankeyOperationNodeDetails? {
      val metadata = service.metadata("com.orbitalhq.kafka.KafkaService")
      val connectionName = metadata.params.get("connectionName") as String?
      if (connectionName == null) {
         logger.warn { "Didn't receive the expected params in the Kafka service metadata.  Expected an annotation named com.orbitalhq.kafka.KafkaService, with a param connectionName" }
         return null
      }
      val topic = operation.metadata("com.orbitalhq.kafka.KafkaOperation").params.get("topic") as String?
      if (topic == null) {
         logger.warn { "Didn't receive the expected params in the Kafka operation metadata.  Expected an annotation named com.orbitalhq.kafka.KafkaOperation, with a param topic" }
         return null
      }
      return KafkaOperationNode(
         connectionName, topic
      )
   }

   private fun isKafkaTopic(service: Service, operation: RemoteOperation): Boolean {
      return operation.hasMetadata("com.orbitalhq.kafka.KafkaOperation")
   }

   private fun isHttpApi(service: Service, operation: RemoteOperation): Boolean {
      return operation.hasMetadata(HttpOperation.NAME)
   }

   private fun buildHttpApi(
      service: Service,
      operation: RemoteOperation,
      operationResult: OperationResult
   ): SankeyOperationNodeDetails? {
      val remoteCall = operationResult.remoteCall

      // Use the address from the metadata, rather than the remote call,
      // as this is templated, and will be consistent between calls.
      val path = operation.metadata(HttpOperation.NAME).params["url"] as String?
      if (path == null) {
         logger.warn { "Could not construct an Http node for ${operation.qualifiedName} as no HttpOperation url metadata was found" }
         return null
      }
      return HttpOperationNode(
         operation.qualifiedName,
         verb = remoteCall.method,
         path = path
      )
   }
}
