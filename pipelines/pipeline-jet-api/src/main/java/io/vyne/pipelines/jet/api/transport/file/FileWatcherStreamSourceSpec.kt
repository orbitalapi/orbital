package io.vyne.pipelines.jet.api.transport.file

import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

object FileWatcherTransport {
   const val TYPE: PipelineTransportType = "fileWatcher"
   val INPUT = FileWatcherStreamSourceSpec.specId
}

data class FileWatcherStreamSourceSpec(
   val path: String,
   val typeName: String,
) : PipelineTransportSpec {
   override val type: PipelineTransportType = "fileWatcher"
   override val description: String = "Watches a directory for new files added"
   override val direction: PipelineDirection = PipelineDirection.INPUT

   companion object {
      val specId = PipelineTransportSpecId(
         type = FileWatcherTransport.TYPE,
         direction = PipelineDirection.INPUT,
         clazz = FileWatcherStreamSourceSpec::class.java
      )
   }
}
