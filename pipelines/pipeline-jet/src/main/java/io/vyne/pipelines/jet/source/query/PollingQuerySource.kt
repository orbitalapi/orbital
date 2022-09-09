package io.vyne.pipelines.jet.source.query

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.spring.VyneProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import javax.annotation.PostConstruct
import javax.annotation.Resource


private const val CAPACITY = 1024

/**
 * Executes a Vyne query on a given interval. While the query results are streamed, it is still a one-off pipeline execution
 * which means that BatchSource is the right choice over StreamSource.
 */
@Component
class PollingQuerySourceBuilder : PipelineSourceBuilder<PollingQueryInputSpec> {

   companion object {
      const val NEXT_SCHEDULED_TIME_KEY = "next-scheduled-time"
   }

   override val sourceType: PipelineSourceType
      get() = PipelineSourceType.Batch

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean {
      return pipelineSpec.input is PollingQueryInputSpec
   }

   override fun buildBatch(
      pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>,
      inputType: Type?
   ): BatchSource<MessageContentProvider> {
      return SourceBuilder.batch("query-poll") { context ->
         PollingQuerySourceContext(context.logger(), pipelineSpec)
      }
         .fillBufferFn { context: PollingQuerySourceContext, buffer: SourceBuffer<MessageContentProvider> ->
            context.fillBuffer(buffer)
         }
         .build()
   }

   override fun getEmittedType(
      pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>,
      schema: Schema
   ): QualifiedName? {
      return null
   }
}

@SpringAware
class PollingQuerySourceContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>
) {
   @PostConstruct
   fun runQuery() {
      val scope = CoroutineScope(Dispatchers.Default)
      scope.launch {
         val vyne = vyneProvider.createVyne()
         vyne.query(pipelineSpec.input.query)
            .results
            .map { TypedInstanceContentProvider(it) }
            .onEach { queue.add(it) }
            .onCompletion { isDone = true }
            .catch { isDone = true }
            .launchIn(scope)
      }
   }

   @Resource
   lateinit var vyneProvider: VyneProvider

   private val queue: BlockingQueue<MessageContentProvider> = ArrayBlockingQueue(CAPACITY)
   private val tempBuffer: MutableList<MessageContentProvider> = mutableListOf()
   private var isDone = false

   fun fillBuffer(buffer: SourceBuffer<MessageContentProvider>) {
      logger.finest("Writing ${queue.size} items into the polling query sink's buffer.")
      queue.drainTo(tempBuffer)
      tempBuffer.forEach(buffer::add)
      tempBuffer.clear()
      if (isDone) {
         logger.info("Polling query execution finished. Closing buffer.")
         buffer.close()
      }
   }

}
