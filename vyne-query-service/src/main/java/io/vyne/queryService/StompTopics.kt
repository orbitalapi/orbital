package io.vyne.queryService

object StompTopics {
   val ALL_QUERY_STATUS_UPDATES = "/topic/runningQueryUpdates"
   fun QUERY_STATUS_UPDATES(queryId:String) = "/topic/runningQueryUpdates/$queryId"
}
