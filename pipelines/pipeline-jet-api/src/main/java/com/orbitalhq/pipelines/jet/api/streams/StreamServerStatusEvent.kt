package com.orbitalhq.pipelines.jet.api.streams

data class StreamServerStatusEvent(
   val clusterSize: Int,
   val streams: List<StreamStatus>
) {
   companion object {
      val UKNOWN = StreamServerStatusEvent(
         -1, emptyList()
      )
   }
}
