package com.orbitalhq.query

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.QualifiedNameAsStringDeserializer
import com.orbitalhq.schemas.QualifiedNameAsStringSerializer
import com.orbitalhq.utils.Ids
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * A remote call models the response from a remote operation.
 * It's possible there are multiple responses for an operation, if the operation
 * returns a stream, or if application code converts a non-streaming response to a streaming
 * response.
 * Therefore, remoteCallId and responseId are separate, to allow for this one-to-many relationship.
 *
 */
data class RemoteCall(
   /**
    * Use a consistent remoteCallId for all responses.
    */
   val remoteCallId: String = Ids.fastUuid(),
   val responseId: String = Ids.fastUuid(),
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val service: QualifiedName,
   val address: String,
   val operation: String,
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val responseTypeName: QualifiedName,

   @Deprecated(message = "Use a dedicated exchange type")
   val method: String = "",
   @Deprecated(message = "Use a dedicated exchange type")
   val requestBody: Any? = null,

   @Deprecated(message = "Use a dedicated exchange type")
   val resultCode: Int = -1,
   val durationMs: Long,
   val timestamp: Instant,

   // Nullable for now, as we transition this to being stored.
   // After a while, let's make this stricter.
   val responseMessageType: ResponseMessageType?,

   val exchange: RemoteCallExchangeMetadata,

   @get:JsonIgnore
   val response: Any?,
   val isFailed: Boolean = false
) {
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   @JsonDeserialize(using = QualifiedNameAsStringDeserializer::class)
   val operationQualifiedName: QualifiedName = OperationNames.qualifiedName(service.fullyQualifiedName, operation)


   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val responseTypeDisplayName: String
      get() {
         return responseTypeName.shortDisplayName
      }

   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val serviceDisplayName: String
      get() {
         return service.shortDisplayName
      }
}

enum class ResponseMessageType {
   /**
    * The default restful response- a full payload message
    */
   FULL,

   /**
    * An event from a streaming based protocol - either a message off a queue,
    * or a SSE / Websocket message in an HTTP request
    */
   EVENT
}


/**
 * Models transport-specific metadata we wish to capture.
 *
 * Rather than trying to cram everything into a single,
 * general-purpose RemoteCall object,
 * we use Exhcnage metadata to capture the specifics in a
 * well modelled manner.
 *
 * eg: capture verb, response code, headers etc for HTTP,
 * other stuff for db.
 *
 */

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
   JsonSubTypes.Type(value = HttpExchange::class, name = "Http"),
   JsonSubTypes.Type(value = SqlExchange::class, name = "Sql"),
   JsonSubTypes.Type(value = MessageStreamExchange::class, name = "MessageStream"),
   JsonSubTypes.Type(value = EmptyExchangeData::class, name = "None")
)
sealed class RemoteCallExchangeMetadata {
   abstract val requestBody: String?
}

@Serializable
data class HttpHeaders(
   val requestHeaders: Map<String, List<String>>,
   val responseHeaders: Map<String, List<String>>
) {
   companion object {
      fun empty() = HttpHeaders(emptyMap(), emptyMap())
   }
}

@Serializable
data class HttpExchange(
   val url: String,
   val verb: String,
   override val requestBody: String?,

   val responseCode: Int,
   val responseSize: Int,
   val headers: com.orbitalhq.query.HttpHeaders
) : RemoteCallExchangeMetadata()

@Serializable
data class SqlExchange(
   val sql: String,
   val recordCount: Int,
   val verb: String
) : RemoteCallExchangeMetadata() {
   override val requestBody: String = sql
}

@Serializable
data class CacheExchange(
   val connectionName: String,
   val cacheName: String,
   /**
    * When a simple key lookup, provide the key.
    * If querying, provide the query statement
    */
   val cacheKeyOrStatement: String,
   val verb: CacheOperationVerb,
   val cacheType: CacheType,
   val recordCount: Int,
) : RemoteCallExchangeMetadata() {
   override val requestBody: String? = null

   enum class CacheOperationVerb {
      /**
       * Find a specific value by key
       */
      LOOKUP,

      /**
       * Execute a query against the cache
       */
      QUERY,

      /**
       * Return the full cache
       */
      FIND_ALL,

      UPDATE,

      DELETE
   }

   enum class CacheType {
      Hazelcast,
      Redis,
      Unknown
   }
}


@Serializable
data class MessageStreamExchange(
   val topic: String
) : RemoteCallExchangeMetadata() {
   override val requestBody: String? = null
}

@Serializable
data class ObjectStoreExchange(
   val bucketName: String,
   val filePattern: String? = null,
   val sql: String? = null
) : RemoteCallExchangeMetadata() {
   override val requestBody: String? = null
}

@Serializable
object EmptyExchangeData : RemoteCallExchangeMetadata() {
   override val requestBody: String? = null
}
