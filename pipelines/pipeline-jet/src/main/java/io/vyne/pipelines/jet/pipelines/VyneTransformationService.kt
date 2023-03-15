package io.vyne.pipelines.jet.pipelines

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.embedded.EmbeddedVyneClientWithSchema
import io.vyne.models.TypedCollection
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.TypedInstanceContentProvider
import io.vyne.schemas.QualifiedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import javax.annotation.Resource
import kotlin.coroutines.CoroutineContext

/**
 * A coroutine scope that defers work back to the Unconfined dispatcher.
 * This means that work that starts uses the calling thread until such time as it
 * hits a suspension point, when it defers to that suspension code.
 *
 * Since the Vyne Transformation job is running in a thread from a CompletableFuture threadpool,
 * we want to attach to the calling thread, until the Vyne code defers.
 *
 * This seems like a reasonable approach, but need validation.  Have pinged the Koroutine channel for a review
 */
internal object VyneTransformationScope : CoroutineScope {
   override val coroutineContext: CoroutineContext
      get() = Dispatchers.Unconfined
}


/**
 * A Hazelcast Jet service which wraps the Vyne transformation phase of a pipeline.
 * As per the Jet APIs, must return a CompletableFuture<>.  So, this class does some
 * framework hopping to link the CompletableFuture<> APIs with our Kotlin Coroutine APIs.
 *
 * Loggers are Jet loggers, to ensure context is correctly captured.
 */
class VyneTransformationService(
   val vyneClient: EmbeddedVyneClientWithSchema,
   val logger: ILogger
) {
   companion object {
      fun serviceFactory(): ServiceFactory<VyneStageContext, VyneTransformationService> {
         return ServiceFactory.withCreateContextFn { context ->
            VyneStageContext()
         }.withCreateServiceFn { context, vyneStageContext ->
            VyneTransformationService(vyneStageContext.vyneClient, context.logger())
         }
      }
   }

   fun transformWithVyne(
      messageContentProvider: MessageContentProvider,
      inputType: QualifiedName,
      outputType: QualifiedName
   ): CompletableFuture<TypedInstanceContentProvider> {
      val input = try {
         messageContentProvider.readAsTypedInstance(vyneClient.schema.type(inputType), vyneClient.schema)
      } catch (e: Exception) {
         logger.severe("Failed to read inbound message as type ${inputType.longDisplayName} - ${e.message}", e)
         return CompletableFuture.failedFuture(e)
      }

      val futureContentProvider: CompletableFuture<TypedInstanceContentProvider> = VyneTransformationScope.future {
         val transformationResult = try {
            /**
             * TODO
             * Move to use remote Vyne client over the embedded one as soon as we support passing input
             * as part of the request. Currently it is not possible to send the data (i.e. "given { <data here> }")
             * as part of the request, so we have to use the embedded client which means the transformation happens on
             * the pipelines instance and not in query server which is suboptimal.
             */
            vyneClient.from(input)
               .build(outputType.parameterizedName)
               .results.toList()
         } catch (e: Exception) {
            logger.severe(
               "Transforming input message of type ${inputType.longDisplayName} to ${outputType.longDisplayName} failed with exception ${e.message}",
               e
            )
            return@future e.left()
         }

         logger.info("Transforming input message of type ${inputType.longDisplayName} to ${outputType.longDisplayName} completed with ${transformationResult.size} results")

         val typedInstance = when {
            transformationResult.isEmpty() -> TypedCollection.empty(vyneClient.schema.type(outputType))
            transformationResult.size == 1 -> transformationResult.first()
            else -> TypedCollection.from(transformationResult)
         }
         TypedInstanceContentProvider(
            typedInstance,
            sourceMessageMetadata = messageContentProvider.sourceMessageMetadata
         ).right()
      }.thenApply { either ->
         // Outside the Coroutine scope, we need to resurface any exceptions,
         // so the CompletableFuture will show as failed.
         either.getOrHandle { throw it }
      }
      return futureContentProvider
   }
}

@SpringAware
class VyneStageContext {
   @Resource
   lateinit var vyneClient: EmbeddedVyneClientWithSchema
}
