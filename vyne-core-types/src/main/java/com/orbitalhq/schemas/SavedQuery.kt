package com.orbitalhq.schemas

import com.orbitalhq.VersionedSource
import com.orbitalhq.scheduler.ScheduleConfiguration
import lang.taxi.annotations.HttpOperation
import lang.taxi.annotations.WebsocketOperation
import lang.taxi.query.QueryMode
import lang.taxi.query.TaxiQlQuery

data class SavedQuery(
   val name: QualifiedName,
   val sources: List<VersionedSource>,
   val queryKind: QueryKind,
   val publications: List<QueryPublication>
) {

   // for backwards compatibility
   val httpEndpoint: HttpOperation? = publications
      .filterIsInstance<HttpEndpointPublication>()
      .firstOrNull()?.httpOperation

   // for backwards compatibility
   val websocketOperation: WebsocketOperation? = publications
      .filterIsInstance<WebsocketPublication>()
      .firstOrNull()?.websocketOperation

   val schedule: ScheduleConfiguration? = publications
      .filterIsInstance<ScheduledQueryPublication>()
      .firstOrNull()?.config

   enum class QueryKind {
      Stream,
      Query;

      companion object {
         fun forQueryMode(queryMode: QueryMode): QueryKind {
            return when (queryMode) {
               QueryMode.STREAM -> Stream
               else -> Query
            }
         }
      }
   }
}


/**
 * Marker interface for different mechanisms that a query is
 * "published", or made available. Each query may have 0-to-many
 * publications
 */
interface QueryPublication {
   val kind: String
}

data class HttpEndpointPublication(val httpOperation: HttpOperation) : QueryPublication {
   override val kind: String = "HttpEndpoint"
}

data class WebsocketPublication(val websocketOperation: WebsocketOperation) : QueryPublication {
   override val kind: String = "Websocket"
}

object BackgroundStreamPublication : QueryPublication {
   override val kind: String = "BackgroundStream"
   fun fromQuery(query: TaxiQlQuery): BackgroundStreamPublication? {
      return if (query.queryMode == QueryMode.STREAM) {
         BackgroundStreamPublication
      } else null
   }
}

data class ScheduledQueryPublication(val config: ScheduleConfiguration) : QueryPublication {
   override val kind: String = "Scheduled"
}


object QueryPublications {
   fun fromQuery(query: TaxiQlQuery): List<QueryPublication> {
      return listOfNotNull(
         HttpOperation.fromQuery(query)?.let { HttpEndpointPublication(it) },
         WebsocketOperation.fromQuery(query)?.let { WebsocketPublication(it) },
         BackgroundStreamPublication.fromQuery(query),
         ScheduleConfiguration.fromQuery(query)?.let { ScheduledQueryPublication(it) }
      )
   }
}
