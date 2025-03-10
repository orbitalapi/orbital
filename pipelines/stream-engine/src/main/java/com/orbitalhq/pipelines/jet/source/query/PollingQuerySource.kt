package com.orbitalhq.pipelines.jet.source.query

import com.hazelcast.jet.pipeline.BatchSource
import com.hazelcast.jet.pipeline.SourceBuilder
import com.hazelcast.jet.pipeline.SourceBuilder.SourceBuffer
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.VyneClient
import com.orbitalhq.VyneProvider
import com.orbitalhq.auth.EmptyAuthenticationToken
import com.orbitalhq.auth.authentication.ExecutionPrincipalAuthenticationService
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
import com.orbitalhq.query.tagsOf
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.time.Duration.Companion.seconds

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
         QueryBufferingPipelineContext(
            context.logger(),
            pipelineSpec,
            context.jobId(),
            QueryBufferingPipelineContext.BufferMode.Batch
         )
      }
         .fillBufferFn { context: QueryBufferingPipelineContext, buffer: SourceBuffer<MessageContentProvider> ->
            context.drainTo(buffer)
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
   val mode: BufferMode
) {
   enum class BufferMode {
      Stream, Batch;
   }

   private lateinit var queryJob: Job
   private lateinit var querySubscription: Disposable

   /**
    * Holds an exception thrown by the query execution layer.
    * We rethrow this on the next poll from Hazelcast Jet, so that the failure is
    * propogated into the Jet excecution context
    */
   private var executionException: Throwable? = null

   private fun queryIsValid():Boolean {
      return try {
         vyneClient.compile(pipelineSpec.input.query)
         true
      } catch (e:Exception) {
         logger.warning("The provided pipeline query fails to compile: ${e.message}")
         false
      }

   }
   @PostConstruct
   fun runQuery() {
      val scope = CoroutineScope(Dispatchers.Default)

      queryJob = scope.launch {

         // When running a distributed query, we can receive the query via Jet before we receive
         // the schema required to compile it. (ie., the job was compiled and started on another node).
         // We need to protect against that, as if a compilation error is thrown when we're launching
         // the query, the job fails.
         // ORB-927 has been raised to refactor this to use QueryMessage, which is self-contained
         // and eliminates the race condition
         while (!queryIsValid()) {
            logger.info("Waiting 5 seconds and will try again")
            delay(5.seconds)
         }
         val principalOrEmpty = if (executionPrincipalAuthenticationService.isPresent) {
            executionPrincipalAuthenticationService.get().loadPrincipal()
         } else {
            Mono.just(EmptyAuthenticationToken)
         }.awaitSingle()
         val principal = EmptyAuthenticationToken.nullIfEmpty(principalOrEmpty)
         querySubscription = vyneClient.query<TypedInstance>(
            pipelineSpec.input.query,
            tagsOf().queryStream(pipelineSpec.name).tags(),
            principal
         )
            .map {
               TypedInstanceContentProvider(
                  it,
                  sourceMessageMetadata = PollingQuerySourceMetadata(jobId.toString())
               )
            }
            .doOnComplete { isDone = true }
            .doOnError { error ->
               logger.warning("An exception was thrown in the query execution layer of a streaming job: ${error.message}. This message will be propagated back to Jet on the next poll.")
               executionException = error
               isDone = true
            }
            .subscribe {
               val wasQueued = queue.offer(it)
               if (!wasQueued) {
                  logger.warning("Failed to append query result to the result queue.  Is the buffer full? Current size is ${queue.size}")
               }
            }


      }
   }

   fun terminate() {
      runBlocking {
         logger.info("Terminating running TaxlQL query")
         querySubscription.dispose()
         queryJob.cancelAndJoin()
      }
   }

   @Resource
   lateinit var executionPrincipalAuthenticationService: Optional<ExecutionPrincipalAuthenticationService>

   @Resource
   lateinit var vyneClient: VyneClient

   private val queue: BlockingQueue<MessageContentProvider> = ArrayBlockingQueue(CAPACITY)
   private var isDone = false

   fun drainTo(buffer: SourceBuffer<MessageContentProvider>) {
      // If something went wrong on the query execution thread, throw it now.
      // This ensures that the exception is propigated on Jet's execution threads,
      // so job states etc., are updated.
      executionException?.let { throw it }
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
