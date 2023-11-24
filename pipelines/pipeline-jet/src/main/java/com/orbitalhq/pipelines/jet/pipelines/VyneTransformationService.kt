package com.orbitalhq.pipelines.jet.pipelines

import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.hazelcast.jet.pipeline.ServiceFactory
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.embedded.EmbeddedVyneClientWithSchema
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import jakarta.annotation.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.future
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.types.Arrays
import lang.taxi.types.PrimitiveType
import java.util.concurrent.CompletableFuture
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
      outputType: QualifiedName?,
      transformation: TaxiQLQueryString?
   ): CompletableFuture<TypedInstanceContentProvider> {
      val input = try {
         messageContentProvider.readAsTypedInstance(vyneClient.schema.type(inputType), vyneClient.schema)
      } catch (e: Exception) {
         logger.severe("Failed to read inbound message as type ${inputType.longDisplayName} - ${e.message}", e)
         return CompletableFuture.failedFuture(e)
      }
      val pipelineDescription = if (transformation != null) {
         "Transforming input message of type ${inputType.longDisplayName} with TaxiQL Query"
      } else {
         "Transforming input message of type ${inputType.longDisplayName} to ${outputType!!.longDisplayName}"
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
            if (transformation != null) {
               val json = Jackson.taxiQlObjectWriter
                  .writerWithDefaultPrettyPrinter().writeValueAsString(input.toRawObject())
               val givenClause = "given { ${inputType.parameterizedName} = $json }"
               val query = "$givenClause\n$transformation"
               logger.fine("Transformation query: $query")
               val transformationResult = try {
                  vyneClient.queryAsTypedInstance(query)
                     .collectList().block()!!
               } catch (e: Exception) {
                  logger.warning(
                     "An exception was thrown executing a TaxiQL transformation query.  This record will be skipped.  Offending query: \n$query",
                     e
                  )
                  emptyList()
               }
               transformationResult
            } else {
               // Verified in the earlier stage that outputType != null if a transformation is not provided
               vyneClient.from(input)
                  .build(outputType!!.parameterizedName)
                  .results.toList()
            }

         } catch (e: Exception) {
            logger.severe(
               "$pipelineDescription failed with exception ${e.message}",
               e
            )
            return@future e.left()
         }

         logger.info("$pipelineDescription completed with ${transformationResult.size} results")

         val typedInstance = when {
            // TODO : This isn't great - we need a type to construct an empty schema.  We might not have one if a transformation query is used.
            // We can get a TaxiType by compiling the query, but can't easily get the correspodning Vyne type.
            // I dont think the type is material given the collection is empty, so falling back to ANY here.

            transformationResult.isEmpty() -> TypedCollection.empty(
               vyneClient.schema.type(
                  outputType ?: Arrays.arrayOf(
                     PrimitiveType.ANY
                  ).toVyneQualifiedName()
               )
            )

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
