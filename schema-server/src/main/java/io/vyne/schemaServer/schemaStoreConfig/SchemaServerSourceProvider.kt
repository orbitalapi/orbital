package io.vyne.schemaServer.schemaStoreConfig

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.rSocketSchemaPublisher.RSocketPublisherKeepAliveStrategyMonitor
import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaApi.SchemaSourceProvider
import io.vyne.schemaPublisherApi.ExpiringSourcesStore
import io.vyne.schemaPublisherApi.KeepAliveStrategyMonitor
import io.vyne.schemaPublisherApi.PublisherConfiguration
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaPublisherApi.SourceSubmissionResponse
import io.vyne.schemaPublisherApi.VersionedSourceSubmission
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger { }

/**
 * When --vyne.schema.publicationMethod is not specified, Schema Server starts exposing this class which acts like
 * a central Schema Management facade. Schema Consumer applications will consume schemas through either HTTP GET requests
 * or RSOCKET stream requests both of which will be handled by this class. Likewise, Schema publisher apps will submit their schemas
 * through HTTP POST or RSOCKET request-reply requests.
 */
@ConditionalOnExpression("T(org.springframework.util.StringUtils).isEmpty('\${vyne.schema.publicationMethod:}')")
@RestController
@RequestMapping("/api/schemas/taxi")
class SchemaServerSourceProvider(
   keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>,
   private val rSocketPublisherKeepAliveStrategyMonitor: RSocketPublisherKeepAliveStrategyMonitor,
   private val objectMapper: ObjectMapper,
   // internal for testing purposes.
   internal val taxiSchemaStoreWatcher: ExpiringSourcesStore = ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors),
   private val validatingStore: ValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient(),
   private val schemaUpdateNotifier: SchemaUpdateNotifier = LocalSchemaNotifier(validatingStore)
) :
   SchemaSourceProvider, InitializingBean {


   private val emitFailureHandler = Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult
         == Sinks.EmitResult.FAIL_NON_SERIALIZED)
   }

   @RequestMapping(method = [RequestMethod.POST])
   fun submitSources(@RequestBody submission: VersionedSourceSubmission): Mono<SourceSubmissionResponse> {
      return doSubmitSources(submission)
   }

   @RequestMapping(method = [RequestMethod.GET])
   fun listSchemas(
   ): Mono<SchemaSet> {
      return Mono.just(validatingStore.schemaSet())
   }

   @RequestMapping(path = ["/raw"], method = [RequestMethod.GET])
   fun listRawSchema(): String {
      return validatingStore.schemaSet().rawSchemaStrings.joinToString("\n")
   }

   override fun schemas(): List<Schema> {
      return validatingStore.schemaSet().taxiSchemas
   }

   override fun schemaStrings(): List<String> {
      return validatingStore.schemaSet().rawSchemaStrings
   }

   override fun afterPropertiesSet() {
      logger.info { "Initialised SchemaServerSourceProvider" }
      schemaUpdateNotifier.sendSchemaUpdate()
      taxiSchemaStoreWatcher
         .currentSources
         .subscribe { currentState ->
            logger.info { "Received an update of SchemaSources, submitting to schema store" }
            val result = validatingStore.submitSchemas(currentState.sources, currentState.removedSchemaIds)
            currentState.resultConsumer?.let {
               val errorList = if (result is Either.Left) result.a.errors else emptyList()
               it(Pair(validatingStore.schemaSet(), errorList))
            }
            schemaUpdateNotifier.sendSchemaUpdate()
            //schemaSetSink.emitNext(validatingStore.schemaSet(), emitFailureHandler)
         }
   }

   /**
    * Invoked When an RSocket Client sends the 'SETUP' Frame to the RSocket Schema Server.
    *
    */
   @ConnectMapping
   fun handle(requester: RSocketRequester, @Payload publisherConfigurationStr: String?): Mono<Void> {
      var publisherConfiguration: PublisherConfiguration? = null
      requester.rsocket() // Invoke when the RSocket is closed.
         // A {@code RSocket} can be closed by explicitly calling {@link RSocket#dispose()}
         // or when the underlying transport connection is closed.
         .onClose()
         .doFirst {
            if (publisherConfigurationStr?.isNotBlank() == true) {
               publisherConfiguration =
                  objectMapper.readValue(publisherConfigurationStr, PublisherConfiguration::class.java)
               logger.info { "A Schema Publisher Connected With Configuration => $publisherConfiguration" }
            } else {
               logger.info("A schema consumer is connected ${requester.rsocket().hashCode()}")
            }

         }
         .doOnError {
            logger.warn { "Channel to client $publisherConfigurationStr CLOSED" }
            handleSchemaPublisherDisconnect(publisherConfigurationStr)
         }
         .doFinally {
            logger.info("Client $publisherConfigurationStr DISCONNECTED")
            handleSchemaPublisherDisconnect(publisherConfigurationStr)
         }
         .subscribe()
      return Mono.empty()
   }

   @MessageMapping("stream.vyneSchemaSets")
   fun onSchemaSetSubscriptionRequest(): Flux<SchemaSet> = schemaUpdateNotifier.schemaSetFlux

   @MessageMapping("request.vyneSchemaSubmission")
   fun schemaSubmissionFromRSocket(submission: VersionedSourceSubmission): Mono<SourceSubmissionResponse> {
      return doSubmitSources(submission)
   }

   private fun handleSchemaPublisherDisconnect(publisherConfigurationStr: String?) {
      if (publisherConfigurationStr?.isNotBlank() == true) {
         val publisherConfiguration =
            objectMapper.readValue(publisherConfigurationStr, PublisherConfiguration::class.java)
         rSocketPublisherKeepAliveStrategyMonitor.onSchemaPublisherRSocketConnectionTerminated(publisherConfiguration)
      }
   }

   private fun doSubmitSources(submission: VersionedSourceSubmission): Mono<SourceSubmissionResponse> {
      logger.info { "Received Schema Submission From ${submission.identifier}" }
      val compilationResultSink = Sinks.one<Pair<SchemaSet, List<CompilationError>>>()
      val resultMono = compilationResultSink.asMono().cache()
      taxiSchemaStoreWatcher
         .submitSources(
            submission = submission,
            resultConsumer = { result: Pair<SchemaSet, List<CompilationError>> ->
               compilationResultSink.tryEmitValue(
                  result
               )
            }
         )

      return resultMono.map { (schemaSet, errors) ->
         SourceSubmissionResponse(errors, schemaSet)
      }
   }
}

class SchemaServerSchemaPublisher(
   internal val taxiSchemaStoreWatcher: ExpiringSourcesStore
) : SchemaPublisher {
   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      val compilationResultSink = Sinks.one<Pair<SchemaSet, List<CompilationError>>>()
      val resultMono = compilationResultSink.asMono().cache()
      taxiSchemaStoreWatcher
         .submitSources(
            submission = VersionedSourceSubmission(versionedSources, PublisherConfiguration("SchemaServer")),
            resultConsumer = { result: Pair<SchemaSet, List<CompilationError>> ->
               compilationResultSink.tryEmitValue(
                  result
               )
            }
         )


      val (schemaSet, errors) = resultMono.toFuture().get()
      return when {
         errors.isEmpty() && schemaSet.taxiSchemas.isNotEmpty() -> Either.Right(schemaSet.taxiSchemas.first())
         errors.isEmpty() && schemaSet.taxiSchemas.isEmpty() -> TaxiSchema.empty().right()
         else -> Either.Left(CompilationException(errors))
      }
   }

}
