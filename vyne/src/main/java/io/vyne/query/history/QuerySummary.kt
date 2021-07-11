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
import javax.persistence.Id
import javax.persistence.Lob


@Entity(name = "QUERY_SUMMARY")
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
   @Lob
   @Column(name = "query_json")
   val queryJson: String?,
   @Column(name = "start_time")
   val startTime: Instant,
   @Enumerated(EnumType.STRING)
   @Column(name = "response_status")
   val responseStatus: QueryResponse.ResponseStatus,
   @Column(name = "end_time")
   val endTime: Instant? = null,
   @Column(name = "error_message")
   val errorMessage: String? = null,
   // r2dbc requires an id, which can be set during persistence
   // in order to determine if the row exists
   @Id
   @GeneratedValue
   val id: Long? = null,
   @Lob
   @Column(name = "anonymous_types_json")
   val anonymousTypesJson: String? = null
) {
   @javax.persistence.Transient
   val durationMs = endTime?.let { Duration.between(startTime, endTime).toMillis() }

   @javax.persistence.Transient
   var recordCount: Int = 0

}

@Entity(name = "QUERY_RESULT_ROW")
@Serializable
data class QueryResultRow(
   @Id
   @GeneratedValue
   @Column(name = "row_id")
   val rowId: Long? = null,
   @Column(name = "query_id")
   val queryId: String,
   @Lob
   @Column(name = "json")
   @JsonRawValue
   val json: String,
   @Column(name = "value_hash")
   val valueHash: Int
) {
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
   @Lob
   @Column(name = "data_source_json")
   val dataSourceJson: String
)

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
   @Lob
   @JsonRawValue
   val response: String
)
