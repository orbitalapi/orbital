package io.vyne.pipelines.runner.transport.files

import io.vyne.pipelines.*
import io.vyne.pipelines.runner.transport.PipelineInputTransportBuilder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

object FileWatcherTransport {
   const val TYPE: PipelineTransportType = "file"
}

data class FileWatcherInputSpec(val path: String) : PipelineTransportSpec {
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val type: PipelineTransportType = FileWatcherTransport.TYPE
}

@Component
class FileWatcherInputBuilder : PipelineInputTransportBuilder<FileWatcherInputSpec> {
   override fun canBuild(spec: PipelineTransportSpec): Boolean {
      return spec.type == FileWatcherTransport.TYPE
         && spec.direction == PipelineDirection.INPUT
   }

   override fun build(spec: FileWatcherInputSpec): PipelineInputTransport {
      return FileWatcherInput(spec)
   }
}

class FileWatcherInput(private val spec: FileWatcherInputSpec) : PipelineInputTransport {
   override val feed: Flux<PipelineInputMessage>
      get() = TODO("Not yet implemented")

}


