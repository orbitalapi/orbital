package io.vyne.query.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.TypeNamedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id


@Entity(name = "QUERY_SUMMARY")
@Serializable
data class QuerySummary(
   @Column(name = "query_id")
   val queryId: String,
   @Column(name = "client_query_id")
   val clientQueryId: String,
   @Column(name = "taxi_ql")
   val taxiQl: String?,
   // Note - attempts to use the actual object here (rather than the
   // json) have failed.  Looks like r2dbc support for column-level
   // mappers is still too young.

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
   // r2dbc requires an id, which can be set during persistence
   // in order to determine if the row exists
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long? = null,
   @Column(name = "anonymous_types_json", columnDefinition = "clob")
   val anonymousTypesJson: String? = null,
   @Column(name = "response_type")
   val responseType: String? = null
) : VyneHistoryRecord() {
   @javax.persistence.Transient
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
): VyneHistoryRecord() {
   fun asTypeNamedInstance(mapper: ObjectMapper = Jackson.defaultObjectMapper): TypeNamedInstance {
      return mapper.readValue(json)
   }
}

@Entity(name = "LINEAGE_RECORD")
@Serializable
data class LineageRecord(
   // Data sources must be able to compute a repeatable, consistent id
   // to use for persistence.
   @Id
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
): VyneHistoryRecord()

@Entity(name = "REMOTE_CALL_RESPONSE")
@Serializable
data class RemoteCallResponse(
   // Because remote calls can be streams, it's possible that there are many responses for a single remote call.
   @Id
   @Column(name = "response_id")
   val responseId: String,
   @Column(name = "remote_call_id")
   val remoteCallId: String,
   @Column(name = "query_id")
   val queryId: String,
   @JsonRawValue
   @Column(columnDefinition = "clob")
   val response: String
): VyneHistoryRecord()

/**
 * A SankeyChart is a specific type of visualisation.
 * We persist the data required to build this chart for each query.
 * See LineageSankeyViewBuilder for more details
 */
@Entity(name = "QUERY_SANKEY_ROW")
@Serializable
data class QuerySankeyChartRow(
   @Column(name = "query_id")
   val queryId: String,

   @Enumerated(EnumType.STRING)
   @Column(name = "source_node_type")
   val sourceNodeType: SankeyNodeType,

   @Column(name = "source_node")
   val sourceNode: String,

   @Enumerated(EnumType.STRING)
   @Column(name = "target_node_type")
   val targetNodeType: SankeyNodeType,

   @Column(name = "target_node")
   val targetNode: String,

   @Column(name = "node_count")
   val count: Int,

   // Assigned by the db
   @Column(name = "id")
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long? = null

): VyneHistoryRecord()

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
): VyneHistoryRecord()

@Serializable
sealed class VyneHistoryRecord {
   fun describe(): String {
     return when(this) {
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
data class FlowChartData(val data: List<QuerySankeyChartRow>, val queryId: String): VyneHistoryRecord()

