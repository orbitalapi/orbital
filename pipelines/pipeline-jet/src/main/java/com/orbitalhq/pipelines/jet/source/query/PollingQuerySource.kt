package com.orbitalhq.pipelines.jet.source.query

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.VyneClient
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.MessageSourceWithGroupId
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.query.PollingQueryInputSpec
import com.orbitalhq.pipelines.jet.api.transport.query.TaxiQlQueryPipelineTransportSpec
import com.orbitalhq.pipelines.jet.source.PipelineSourceBuilder
import com.orbitalhq.pipelines.jet.source.PipelineSourceType
import com.orbitalhq.query
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

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
         QueryBufferingPipelineContext(context.logger(), pipelineSpec, context.jobId(), QueryBufferingPipelineContext.BufferMode.Batch)
      }
         .fillBufferFn { context: QueryBufferingPipelineContext, buffer: SourceBuffer<MessageContentProvider> ->
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
class QueryBufferingPipelineContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<out TaxiQlQueryPipelineTransportSpec, *>,
   val jobId: Long,
   val mode:BufferMode
) {
   enum class BufferMode {
      Stream, Batch;
   }
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
   private var isDone = false

   fun fillBuffer(buffer: SourceBuffer<MessageContentProvider>) {
      logger.finest("Writing ${queue.size} items into the polling query sink's buffer.")
      val tempBuffer: MutableList<MessageContentProvider> = mutableListOf()
      queue.drainTo(tempBuffer)
      tempBuffer.forEach(buffer::add)
      if (isDone && mode == BufferMode.Batch) {
         logger.info("Polling query execution finished. Closing buffer.")
         buffer.close()
      }
   }

}
