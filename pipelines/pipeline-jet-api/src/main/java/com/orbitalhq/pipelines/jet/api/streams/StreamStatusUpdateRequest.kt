package com.orbitalhq.pipelines.jet.api.streams

data class StreamStatusUpdateRequest(
   val state: StreamStatus.State
)
