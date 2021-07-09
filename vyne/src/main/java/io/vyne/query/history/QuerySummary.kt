package io.vyne.query.history

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.vyne.models.TypeNamedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryResponse
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.Duration
import java.time.Instant
import javax.persistence.GenerationType
import javax.persistence.GenerationType.*


@Table("query_summary")
data class QuerySummary(
   val queryId: String,
   val clientQueryId: String,
   val taxiQl: String?,
   // Note - attempts to use the actual object here (rather than the
   // json) have failed.  Looks like r2dbc support for column-level
   // mappers is still too young.
   @JsonRawValue
   val queryJson: String?,
   val startTime: Instant,
   val responseStatus: QueryResponse.ResponseStatus,
   val endTime: Instant? = null,
   val errorMessage: String? = null,
   // r2dbc requires an id, which can be set during persistence
   // in order to determine if the row exists
   @Id
   val id: Long? = null,
   val anonymousTypesJson: String? = null
) {
   @Transient
   val durationMs = endTime?.let { Duration.between(startTime, endTime).toMillis() }

   @Transient
   var recordCount: Int = 0

}

@Table
data class QueryResultRow(
   @Id
   val rowId: Long? = null,
   val queryId: String,
   @JsonRawValue
   val json: String,
   val valueHash: Int
) {
   fun asTypeNamedInstance(mapper: ObjectMapper = Jackson.defaultObjectMapper): TypeNamedInstance {
      return mapper.readValue(json)
   }
}

@Table
data class LineageRecord(
   // Data sources must be able to compute a repeatable, consistent id
   // to use for persistence.
   @Id
   val dataSourceId: String,
   val queryId: String,
   val dataSourceType: String,
   @JsonRawValue
   @JsonProperty("dataSource")
   val dataSourceJson: String
) : Persistable<String> {
   @JsonIgnore
   override fun getId(): String {
      return dataSourceId
   }

   // Always return true, as we don't support updating these,
   // so writes should always be new.
   @JsonIgnore
   override fun isNew(): Boolean {
      return true
   }
}

@Table("remote_call_response")
data class RemoteCallResponse(
   // Because remote calls can be streams, it's possible that there are many responses for a single remote call.
   @Id
   val responseId: String,
   val remoteCallId: String,
   val queryId: String,
   @JsonRawValue
   val response: String
) : Persistable<String> {

   override fun getId(): String {
      return responseId
   }

   // Always return true, as we don't support updating these,
   // so writes should always be new.
   @JsonIgnore
   override fun isNew(): Boolean {
      return true
   }

}
