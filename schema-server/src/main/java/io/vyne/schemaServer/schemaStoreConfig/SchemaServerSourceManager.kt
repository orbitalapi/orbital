package io.vyne.schemaServer.schemaStoreConfig

import arrow.core.Either
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schema.publisher.rsocket.RSocketPublisherKeepAliveStrategyMonitor
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.*
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Ids
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Primary
import org.springframework.messaging.handler.annotation.MessageMapping
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
@Primary
class SchemaServerSourceManager(
   keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>,
   private val rSocketPublisherKeepAliveStrategyMonitor: RSocketPublisherKeepAliveStrategyMonitor,
   // internal for testing purposes.
   internal val taxiSchemaStoreWatcher: ExpiringSourcesStore = ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors),
   private val validatingStore: ValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient(),
   private val schemaUpdateNotifier: SchemaUpdateNotifier = LocalSchemaNotifier(validatingStore)
) :
   SchemaSourceProvider, SchemaPublisherTransport {


   @RequestMapping(method = [RequestMethod.POST])
   fun submitSources(@RequestBody submission: VersionedSourceSubmission): Mono<SourceSubmissionResponse> {
      return Mono.just(doSubmitSources(submission))
   }

   @RequestMapping(method = [RequestMethod.GET])
   fun listSchemas(
   ): Mono<SchemaSet> {
      return Mono.just(validatingStore.schemaSet)
   }

   @RequestMapping(path = ["/raw"], method = [RequestMethod.GET])
   fun listRawSchema(): String {
      return validatingStore.schemaSet.rawSchemaStrings.joinToString("\n")
   }

   override val versionedSources: List<VersionedSource>
      get() {
         return validatingStore.schemaSet.allSources
      }

   init {
      logger.info { "Initialised SchemaServerSourceProvider" }
      schemaUpdateNotifier.sendSchemaUpdate()
      taxiSchemaStoreWatcher
         .currentSources
         .subscribe { currentState ->
            logger.info { "Received an update of SchemaSources, submitting to schema store" }
            submitToValidatingStore(currentState)
         }
   }

   private fun submitToValidatingStore(currentState: SourcesUpdatedMessage): Either<CompilationException, Schema> {
      val result = validatingStore.submitSchemas(currentState.sources, currentState.removedSchemaIds)
      if (result.isRight()) {
         schemaUpdateNotifier.sendSchemaUpdate()
      }
      return result
   }

   private val connections: MutableMap<RSocketRequester, String> = mutableMapOf()

   /**
    * Invoked when an RSocket client first establishes a connection
    *
    */
   @ConnectMapping
   fun handle(requester: RSocketRequester): Mono<Void> {
      val rsocketId = connections.getOrPut(requester) {
         val rsocketId = Ids.id("rsocket-")
         logger.info { "Received new RSocket connection, assigned id $rsocketId" }
         rsocketId
      }
      requester.rsocket() // Invoke when the RSocket is closed.
         // A {@code RSocket} can be closed by explicitly calling {@link RSocket#dispose()}
         // or when the underlying transport connection is closed.
         .onClose()
         .doOnError {
            logger.warn(it) { "Channel to RSocket client $rsocketId closed with error" }
            handleSchemaPublisherDisconnect(rsocketId)
         }
         .doFinally {
            logger.info("RSocket client $rsocketId disconnected")
            handleSchemaPublisherDisconnect(rsocketId)
         }
         .subscribe()
      return Mono.empty()
   }

   @MessageMapping(RSocketRoutes.SCHEMA_UPDATES)
   fun onSchemaSetSubscriptionRequest(): Flux<SchemaSet> = schemaUpdateNotifier.schemaSetFlux

   @MessageMapping(RSocketRoutes.SCHEMA_SUBMISSION)
   fun schemaSubmissionFromRSocket(
      requester: RSocketRequester,
      submission: VersionedSourceSubmission
   ): Mono<SourceSubmissionResponse> {
      val rsocketId = connections.get(requester) ?: error("Unknown rsocket attempting to submit schemas")
      logger.info { "Received schema submission: $rsocketId with publisherId ${submission.publisherId}" }
      // We associate the submission with the RSocket, so we can clean up when its disconnected,
      // so swap out the id now:
      val rsocketSubmission = submission.copy(publisherId = rsocketId)
      return Mono.just(doSubmitSources(rsocketSubmission))
   }

   private fun handleSchemaPublisherDisconnect(rsocketId: String) {
      rSocketPublisherKeepAliveStrategyMonitor.onSchemaPublisherRSocketConnectionTerminated(
         PublisherConfiguration(
            rsocketId
         )
      )
   }

   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      val submissionResponse = doSubmitSources(VersionedSourceSubmission(versionedSources, "SchemaServer"))
      return submissionResponse.asEither()
   }

   private fun doSubmitSources(submission: VersionedSourceSubmission): SourceSubmissionResponse {
      logger.info { "Received Schema Submission From ${submission.publisherId}" }
      val sourcesUpdatedMessage = taxiSchemaStoreWatcher
         .submitSources(
            submission = submission,
            emitUpdateMessage = false // We handle calling the validating store ourselves,
         )
      val compilationResult = submitToValidatingStore(sourcesUpdatedMessage)
         .map { validatingStore.schemaSet }
      return SourceSubmissionResponse.fromEither(compilationResult)
   }

}

//class SchemaServerSchemaPublisher(
//   internal val taxiSchemaStoreWatcher: ExpiringSourcesStore
//) : SchemaPublisherTransport {
//   override fun submitSchemas(
//      versionedSources: List<VersionedSource>,
//      removedSources: List<SchemaId>
//   ): Either<CompilationException, Schema> {
//      val compilationResultSink = Sinks.one<Pair<SchemaSet, List<CompilationError>>>()
//      val resultMono = compilationResultSink.asMono().cache()
//      taxiSchemaStoreWatcher
//         .submitSources(
//            submission = VersionedSourceSubmission(versionedSources, "SchemaServer"),
//            resultConsumer = { result: Pair<SchemaSet, List<CompilationError>> ->
//               compilationResultSink.tryEmitValue(
//                  result
//               )
//            }
//         )
//
//
//      val (schemaSet, errors) = resultMono.toFuture().get()
//      return when {
//         errors.isEmpty() && schemaSet.taxiSchemas.isNotEmpty() -> Either.Right(schemaSet.taxiSchemas.first())
//         errors.isEmpty() && schemaSet.taxiSchemas.isEmpty() -> TaxiSchema.empty().right()
//         else -> Either.Left(CompilationException(errors))
//      }
//   }
//
//}