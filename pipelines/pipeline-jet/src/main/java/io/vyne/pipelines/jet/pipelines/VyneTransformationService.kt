package io.vyne.pipelines.jet.pipelines

import arrow.core.Either
import arrow.core.getOrHandle
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.models.TypedCollection
import io.vyne.pipelines.ConsoleLogger
import io.vyne.pipelines.MessageContentProvider
import io.vyne.pipelines.TypedInstanceContentProvider
import io.vyne.schemas.QualifiedName
import io.vyne.spring.VyneProvider
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
 * A HazelcastJet service which wraps the Vyne transformation phase of a pipeline.
 * As per the Jet API's, must return a CompletableFuture<>.  So, this class does some
 * framework hopping to link the CompletableFuture<> API's with our Kotlin Coroutine APIs.
 *
 * Loggers are Jet loggers, to ensure context is correctly captured.
 */
class VyneTransformationService(
   val vyneProvider: VyneProvider,
   val logger: ILogger
) {
   companion object {
      fun serviceFactory(): ServiceFactory<VyneStageContext, VyneTransformationService> {
         return ServiceFactory.withCreateContextFn { context ->
            VyneStageContext()
         }.withCreateServiceFn { context, vyneStageContext ->
            VyneTransformationService(vyneStageContext.vyneProvider, context.logger())
         }
      }
   }

   fun transformWithVyne(
      messageContentProvider: MessageContentProvider,
      inputType: QualifiedName,
      outputType: QualifiedName
   ): CompletableFuture<TypedInstanceContentProvider> {
      val vyne = vyneProvider.createVyne()
      val input = try {
         messageContentProvider.readAsTypedInstance(ConsoleLogger, vyne.schema.type(inputType), vyne.schema)
      } catch (e: Exception) {
         logger.severe("Failed to read inbound message as type ${inputType.longDisplayName} - ${e.message}", e)
         return CompletableFuture.failedFuture(e)
      }

      val futureContentProvider: CompletableFuture<TypedInstanceContentProvider> = VyneTransformationScope.future {
         val transformationResult = try {
            vyne.from(input)
               .build(outputType.parameterizedName)
               .results.toList()
         } catch (e: Exception) {
            logger.severe(
               "Transforming input message of type ${inputType.longDisplayName} to ${outputType.longDisplayName} failed with exception ${e.message}",
               e
            )
            return@future Either.left(e)
         }

         logger.info("Transforming input message of type ${inputType.longDisplayName} to ${outputType.longDisplayName} completed with ${transformationResult.size} results")

         val typedInstance = when {
            transformationResult.isEmpty() -> TypedCollection.empty(vyne.schema.type(outputType))
            transformationResult.size == 1 -> transformationResult.first()
            else -> TypedCollection.from(transformationResult)
         }
         Either.right(TypedInstanceContentProvider(typedInstance))
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
   lateinit var vyneProvider: VyneProvider
}
