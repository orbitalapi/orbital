package io.vyne.pipelines.jet.source.file

import com.hazelcast.jet.core.AbstractProcessor
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.StreamSource
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.FormatDetector
import io.vyne.pipelines.jet.api.transport.CsvRecordContentProvider
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.StringContentProvider
import io.vyne.pipelines.jet.api.transport.file.FileWatcherStreamSourceSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.TextFormatUtils
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn
import io.vyne.utils.files.FileSystemChangeEvent
import io.vyne.utils.files.ReactiveWatchingFileSystemMonitor
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Component
import reactor.core.Disposable
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.readText


@Component
class FileWatcherStreamSourceBuilder : PipelineSourceBuilder<FileWatcherStreamSourceSpec> {
   private val formatDetector = FormatDetector.get(listOf(CsvFormatSpec))

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is FileWatcherStreamSourceSpec
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<FileWatcherStreamSourceSpec, *>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.input.typeName.fqn()
   }

   override fun build(
      pipelineSpec: PipelineSpec<FileWatcherStreamSourceSpec, *>,
      inputType: Type?
   ): StreamSource<MessageContentProvider> {

      val (csvModelFormatAnnotation, csvFormat) = TextFormatUtils.getCsvFormat(inputType!!)

      val source =
         SourceBuilder.stream("FileWatcher at ${pipelineSpec.input.path}") { c -> FileChangeEventCollector(pipelineSpec.input.path) }
            .fillBufferFn { eventCollector, u ->
               eventCollector.take(100)
                  .flatMap { fileContents ->

                     // Convert the file contents into a ContentProvider.
                     // If this is a Csv type, we use a CsvRecordContentProvider (1 per line/record), otherwise
                     // a normal StringContentProvider.
                     // This is a bit gross, as the parsing logic inside of Orbital should
                     // understand that we're giving a text record and return T[].
                     // However, it doesn't, for now.
                     if (csvFormat != null) {
                        CSVParser.parse(fileContents, csvFormat).records.map { record ->
                           CsvRecordContentProvider(
                              record,
                              setOfNotNull(csvModelFormatAnnotation!!.nullValue)
                           )
                        }
                     } else {
                        listOf(StringContentProvider(fileContents))
                     }

                  }
                  .forEach { u.add(it) }
            }
            .destroyFn { collector -> collector.stop() }
            .build() as StreamSource<MessageContentProvider>
      return source
   }
}

class FileChangeEventCollector(path: String) {
   private val queue = LinkedList<String>()
   fun take(count: Int): List<String> {
      val elements = mutableListOf<String>()
      while (elements.size < count) {
         val next: String = queue.poll() ?: break
         elements.add(next)
      }
      return elements
   }

   private val subscription: Disposable

   init {
      subscription = ReactiveWatchingFileSystemMonitor(Paths.get(path))
         .startWatching()
         .map { events ->
            events.filter { event -> event.eventType == FileSystemChangeEvent.FileSystemChangeEventType.FileCreated }
         }
         .filter { events -> events.isNotEmpty() }
         .map { events -> events.map { it.path.readText() } }
         .subscribe { sources -> sources.forEach { source -> queue.offer(source) } }
   }

   fun stop() {
      subscription.dispose()
   }
}

class FileWatcherSource(path: String) : AbstractProcessor() {

}
