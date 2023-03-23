package io.vyne.query.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.TypeNamedInstance
import io.vyne.models.json.Jackson
import io.vyne.models.serde.InstantSerializer
import io.vyne.query.HttpExchange
import io.vyne.query.MessageStreamExchange
import io.vyne.query.QueryResponse
import io.vyne.query.RemoteCall
import io.vyne.query.RemoteCallExchangeMetadata
import io.vyne.query.ResponseMessageType
import io.vyne.query.SqlExchange
import io.vyne.schemas.OperationName
import io.vyne.schemas.OperationNames
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringSerializer
import io.vyne.schemas.ServiceName
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.Transient
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant


@Entity(name = "QUERY_SUMMARY")
@Serializable
data class QuerySummary(
   @Column(name = "query_id")
   val queryId: String,
   @Column(name = "client_query_id")
   val clientQueryId: String,
   @Column(name = "taxi_ql")
   @Lob
   val taxiQl: String?,
   // Note - attempts to use the actual object here (rather than the
   // json) have failed.

   @JsonRawValue
   @Column(name = "query_json", columnDefinition = "CLOB(100000)", length = 100000)
   val queryJson: String?,

   @Column(name = "start_time")
   @Serializable(with = InstantKSerialiser::class)
   val startTime: Instant,
   @Enumerated(EnumType.STRING)
   @Column(name = "response_status")
   val responseStatus: QueryResponse.ResponseStatus,
   @Column(name = "end_time")
   @Serializable(with = InstantKSerialiser::class)
   val endTime: Instant? = null,
   @Column(name = "record_count")
   var recordCount: Int? = null,
   @Column(name = "error_message")
   val errorMessage: String? = null,
   // Exists to determine if the row exists
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long? = null,
   @Column(name = "anonymous_types_json", columnDefinition = "clob")
   val anonymousTypesJson: String? = null,
   @Column(name = "response_type")
   val responseType: String? = null
) : VyneHistoryRecord() {
   @Transient
   var durationMs = endTime?.let { Duration.between(startTime, endTime).toMillis() }
}

@Entity(name = "QUERY_RESULT_ROW")
@Serializable
data class QueryResultRow(
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   @Column(name = "row_id")
   val rowId: Long? = null,
   @Column(name = "query_id")
   val queryId: String,
   @Column(name = "json", columnDefinition = "clob")
   @JsonRawValue
   val json: String,
   @Column(name = "value_hash")
   val valueHash: Int
) : VyneHistoryRecord() {
   fun asTypeNamedInstance(mapper: ObjectMapper = Jackson.defaultObjectMapper): TypeNamedInstance {
      return mapper.readValue(json)
   }
}

@Entity(name = "LINEAGE_RECORD")
@Serializable
@Table(
   indexes = [
      Index(name = "ix_dataSource_query", columnList = "data_source_id,query_id", unique = true)
   ]
)
data class LineageRecord(
   // Data sources must be able to compute a repeatable, consistent id
   // to use for persistence.
   @Id
   @Column(name = "record_id")
   val recordId: String,
   @Column(name = "data_source_id")
   val dataSourceId: String,
   @Column(name = "query_id")
   val queryId: String,
   @Column(name = "data_source_type")
   val dataSourceType: String,
   @JsonRawValue
   @JsonProperty("dataSource")
   @Column(name = "data_source_json", columnDefinition = "clob")
   val dataSourceJson: String

) : VyneHistoryRecord() {
   constructor(
      dataSourceId: String,
      queryId: String,
      dataSourceType: String,
      dataSourceJson: String
      // Note: Using an overloaded constructor here, as using a reference with default values
      // threw an exception from the Kotlin compiler. Suspect will be resolved in future kotlin versions
   ) : this("$queryId/$dataSourceId", dataSourceId, queryId, dataSourceType, dataSourceJson)
}

@Entity(name = "REMOTE_CALL_RESPONSE")
@Serializable
data class RemoteCallResponse(
   // Because remote calls can be streams, it's possible that there are many responses for a single remote call.
   @Id
   @Column(name = "response_id")
   override val responseId: String,
   @Column(name = "remote_call_id")
   override val remoteCallId: String,
   @Column(name = "query_id")
   override val queryId: String,
   @Column(name = "address")
   override val address: String,

   @Serializable(InstantSerializer::class)
   @Column(name = "start_time")
   override val startTime: Instant,
   @Column(name = "duration_ms", nullable = true)
   override val durationMs: Long?,
   @Lob
   @Convert(converter = RemoteCallExchangeMetadataJsonConverter::class)
   @Column(name = "exchange")
   override val exchange: RemoteCallExchangeMetadata,

   @Column(name = "operation")
   @Convert(converter = QualifiedNameJpaConverter::class)
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   override val operation: QualifiedName,

   @Column(name = "success")
   override val success: Boolean,

   @JsonRawValue
   @Column(columnDefinition = "clob", nullable = true)
   val response: String?,

   @Column(name = "message_kind")
   @Enumerated(value = EnumType.STRING)
   override val messageKind: ResponseMessageType,

   @Column(name = "response_type")
   @Convert(converter = QualifiedNameJpaConverter::class)
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   override val responseType: QualifiedName

) : BasePartialRemoteCallResponse, VyneHistoryRecord() {


   companion object {
      fun fromRemoteCall(
         remoteCall: RemoteCall,
         queryId: String,
         objectMapper: ObjectMapper,
         includeResponse: Boolean
      ): RemoteCallResponse {
         val responseJson = if (includeResponse) {
            when (remoteCall.response) {
               null -> null
               // It's pretty rare to get a collection here, as the response value is the value before it's
               // been deserialized.  However, belts 'n' braces.
               is Collection<*>, is Map<*, *> -> objectMapper.writeValueAsString(remoteCall.response)
               else -> remoteCall.response.toString()
            }
         } else {
            null
         }


         return RemoteCallResponse(
            responseId = remoteCall.responseId,
            remoteCallId = remoteCall.remoteCallId,
            queryId = queryId,
            address = remoteCall.address,
            response = responseJson,
            startTime = remoteCall.timestamp,
            durationMs = remoteCall.durationMs,
            exchange = remoteCall.exchange,
            operation = remoteCall.operationQualifiedName,
            success = !remoteCall.isFailed,
            messageKind = remoteCall.responseMessageType!!,
            responseType = remoteCall.responseTypeName
         )
      }
   }
}

/**
 * Thin wrapper around PartialRemoteCallResponse
 * to add helper functions for the UI.
 */
data class RemoteCallResponseDto(
   @JsonIgnore
   private val response: PartialRemoteCallResponse
) : PartialRemoteCallResponse by response {
   // UI Helpers
   val method: String
      get() = when (exchange) {
         is HttpExchange -> (exchange as HttpExchange).verb
         is SqlExchange -> "SELECT"
         is MessageStreamExchange -> "SUBSCRIBE"
         else -> "CALL"
      }


   // Use getters, rather than initializers here, as
   // initializers don't appear to run when loading from the db.
   val resultCode: String
      get() = when (exchange) {
         is HttpExchange -> (exchange as HttpExchange).responseCode.toString()
         else -> if (this.success) "OK" else "ERROR"
      }

   val serviceDisplayName: ServiceName
      get() = OperationNames.serviceName(operation)

   val operationName: OperationName
      get() = OperationNames.operationName(operation)

   val displayName: String
      get() = OperationNames.shortDisplayNameFromOperation(operation)

   val responseTypeDisplayName: String
      get() = responseType.shortDisplayName
}


// This interface exists to appease Spring Data JPA.
// We want an interface that is implemented in our actual entity
// and the reference that doesn't contain the response.
// If RemoteCallResponse implements PartialRemoteCallResponse
// then we can't project to it, as Spring Data returns the full
// RemoteCallResponse.

interface BasePartialRemoteCallResponse {
   val responseId: String
   val remoteCallId: String
   val queryId: String
   val address: String
   val startTime: Instant
   val durationMs: Long?
   val exchange: RemoteCallExchangeMetadata
   val operation: QualifiedName
   val success: Boolean
   val messageKind: ResponseMessageType
   val responseType: QualifiedName
}

/**
 * Convenience for UI, which excludes the response (very expensive to send
 * over the wire, so we require the UI to do an explicit request to fetch the response body)
 */
interface PartialRemoteCallResponse : BasePartialRemoteCallResponse {

}


// Has to be an extension function, because Spring Data.
fun PartialRemoteCallResponse.toDto(): RemoteCallResponseDto {
   return RemoteCallResponseDto(this)
}

/**
 * A SankeyChart is a specific type of visualisation.
 * We persist the data required to build this chart for each query.
 * See LineageSankeyViewBuilder for more details
 */
@Entity(name = "QUERY_SANKEY_ROW")
@Serializable
@IdClass(SankeyChartRowId::class)
data class QuerySankeyChartRow(
   @Column(name = "query_id")
   @Id
   val queryId: String,

   @Enumerated(EnumType.STRING)
   @Column(name = "source_node_type")
   @Id
   val sourceNodeType: SankeyNodeType,

   @Column(name = "source_node")
   @Id
   val sourceNode: String,

   @Column(name = "source_operation_data")
   @Lob
   @Convert(converter = AnyJsonConverter::class)
   @Contextual
//When the row is first created, will be a SankeyOperationNodeDetails.  When reading back from the db, will be a Map<String,Any>
   val sourceNodeOperationData: Any? = null,

   @Enumerated(EnumType.STRING)
   @Column(name = "target_node_type")
   @Id
   val targetNodeType: SankeyNodeType,

   @Column(name = "target_node")
   @Id
   val targetNode: String,

   @Column(name = "target_operation_data")
   @Lob
   @Convert(converter = AnyJsonConverter::class)
   @Contextual
   //When the row is first created, will be a SankeyOperationNodeDetails.  When reading back from the db, will be a Map<String,Any>
   val targetNodeOperationData: Any? = null,

   @Column(name = "node_count")
   val count: Int,
) : VyneHistoryRecord()

@Embeddable
@Serializable
data class SankeyChartRowId(
   val queryId: String,
   val sourceNodeType: SankeyNodeType,
   val sourceNode: String,
   val targetNodeType: SankeyNodeType,
   val targetNode: String,
) : java.io.Serializable

@Serializable
enum class SankeyNodeType {
   QualifiedName,
   AttributeName,
   Expression,
   ExpressionInput,
   ProvidedInput
}

@Serializable
data class QueryEndEvent(
   val queryId: String,
   @Serializable(with = InstantKSerialiser::class)
   val endTime: Instant,
   val status: QueryResponse.ResponseStatus,
   val recordCount: Int,
   val message: String? = null
) : VyneHistoryRecord()

@Serializable
sealed class VyneHistoryRecord {
   fun describe(): String {
      return when (this) {
         is QuerySummary -> "QuerySumary $queryId"
         is QueryResultRow -> "QueryResultRow $queryId"
         is RemoteCallResponse -> "RemoteCallResponse $queryId"
         is QueryEndEvent -> "QueryEndEvent $queryId, record count $recordCount"
         is LineageRecord -> "LineageRecord $queryId"
         is FlowChartData -> "FlowChartData $queryId"
         else -> ""
      }
   }
}

@Serializable
data class FlowChartData(val data: List<QuerySankeyChartRow>, val queryId: String) : VyneHistoryRecord()

