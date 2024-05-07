package com.orbitalhq.query.history

import com.orbitalhq.query.CacheExchange
import com.orbitalhq.schemas.QualifiedName
import jakarta.persistence.Converter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

/**
 * A collection of classes which provide operation specific metadata.
 * (eg., for a Kafka operation, it's broker and topic name).
 *
 * This is for usage in the UI
 */
@Serializable
sealed class SankeyOperationNodeDetails(
   val operationType: OperationNodeType,
) {
   /**
    * Returns a name that indicates the actual brand name of the system we're talking to.
    * Eg., for a database, might contain "Postgres", or "Oracle".
    * For a message broker, might contain "RabbitMQ" or "Kafka", etc.
    */
   open val systemProductName:String?
      get() {
         return null
      }
}

@Serializable
data class KafkaOperationNode(
   val connectionName: String,
   val topic: String
) : SankeyOperationNodeDetails(OperationNodeType.KafkaTopic)

@Serializable
data class HttpOperationNode(
   val operationName: QualifiedName,
   val verb: String,
   val path: String
) : SankeyOperationNodeDetails(OperationNodeType.Http)

@Serializable
data class DatabaseNode(
   val connectionName: String,
   val tableNames: List<String>
) : SankeyOperationNodeDetails(OperationNodeType.Database)

@Serializable
data class CacheNode(
   val connectionName: String,
   val cacheName: String,
   val cacheKey: String,
   val verb: CacheExchange.CacheOperationVerb,
   override val systemProductName: String?
) : SankeyOperationNodeDetails(OperationNodeType.Cache) {

}

enum class OperationNodeType {
   KafkaTopic,
   Database,
   Http,
   Cache
}


/**
 * Uses kotlin serialization (not jackson)
 * to ser/de to JSON.
 * Using kotlin here, as we also need to support polymorphic ser/de to CBOL
 * for sending analytic events across the wire.
 */
@Converter
class SankeyOperationNodeDetailsConverter : JsonConverter<SankeyOperationNodeDetails>() {
   companion object {
      private val logger = KotlinLogging.logger {}
      val json = Json { }
   }

   override fun fromJson(json: String): SankeyOperationNodeDetails {
      return Json.decodeFromString(json)
   }

   override fun toJson(attribute: SankeyOperationNodeDetails): String {
      return Json.encodeToString<SankeyOperationNodeDetails>(attribute)
   }

}
