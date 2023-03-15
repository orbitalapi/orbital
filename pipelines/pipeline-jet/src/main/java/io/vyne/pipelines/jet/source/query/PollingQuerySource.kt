package io.vyne.pipelines.jet.source.query

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.VyneClient
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.MessageSourceWithGroupId
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.pipelines.jet.source.PipelineSourceBuilder
import io.vyne.pipelines.jet.source.PipelineSourceType
import io.vyne.query
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
         PollingQuerySourceContext(context.logger(), pipelineSpec, context.jobId())
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

data class PollingQuerySourceMetadata(val jobId: String) : MessageSourceWithGroupId {
   override val groupId = jobId
}

@SpringAware
class PollingQuerySourceContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<PollingQueryInputSpec, *>,
   val jobId: Long
) {
   @PostConstruct
   fun runQuery() {
      val scope = CoroutineScope(Dispatchers.Default)
      scope.launch {
         vyneClient.query<TypedInstance>(pipelineSpec.input.query)
            .map {
               TypedInstanceContentProvider(
                  it,
                  sourceMessageMetadata = PollingQuerySourceMetadata(jobId.toString())
               )
            }
            .doOnComplete { isDone = true }
            .doOnError { error ->
               logger.severe(error.message)
               isDone = true
            }
            .subscribe {
               queue.put(it)
            }
      }
   }

   @Resource
   lateinit var vyneClient: VyneClient

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
