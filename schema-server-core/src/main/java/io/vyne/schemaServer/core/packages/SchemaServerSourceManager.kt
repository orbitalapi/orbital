package io.vyne.schemaServer.core.packages

import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.PackageIdentifier
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.publisher.rsocket.RSocketPublisherKeepAliveStrategyMonitor
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.*
import io.vyne.schema.rsocket.RSocketRoutes
import io.vyne.schemaServer.core.config.LocalSchemaNotifier
import io.vyne.schemaServer.core.config.SchemaUpdateNotifier
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import io.vyne.schemas.Schema
import io.vyne.utils.Ids
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
import reactor.core.scheduler.Schedulers

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
   fun submitSources(@RequestBody submission: SourcePackage): Mono<SourceSubmissionResponse> {
      return Mono.just(submitPackage(submission).asSourceSubmissionResponse())
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

   override val packages: List<SourcePackage>
      get() {
         return validatingStore.schemaSet.packages
      }

   override val versionedSources: List<VersionedSource>
      get() {
         return validatingStore.schemaSet.allSources
      }

   init {
      logger.info { "Initialised SchemaServerSourceProvider" }
      schemaUpdateNotifier.emitCurrentSchemaSet()
//      taxiSchemaStoreWatcher
//         .currentSources
//         .publishOn(Schedulers.boundedElastic())
//         .subscribe { currentState ->
//            logger.info { "Received an update of SchemaSources, submitting to schema store" }
//            submitToValidatingStore(currentState)
//         }
   }

   private fun submitToValidatingStore(updates: PackagesUpdatedMessage): Either<CompilationException, Schema> {
      val oldSchema = validatingStore.schemaSet.schema
      val result = validatingStore.submitUpdates(updates)
      // Always send the notification, even if the current state is broken.
      // Previously, we only used to send if the state was valid, but this means that broken schemas aren't
      // visible in the UI
      schemaUpdateNotifier.emitCurrentSchemaSet()
      schemaUpdateNotifier.buildAndSendSchemaUpdated(updates, oldSchema)
      return result
   }

   private val connections: MutableMap<RSocketRequester, TransportConnectionId> = mutableMapOf()

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
   fun onSchemaSetSubscriptionRequest(): Flux<SchemaSet> {
      return schemaUpdateNotifier
         .schemaSetFlux
         .doOnEach { signal ->
            logger.info { "Publishing SchemaSet generation ${signal.get()?.generation} on RSocket" }
         }
   }

   @MessageMapping(RSocketRoutes.SCHEMA_SUBMISSION)
   fun schemaSubmissionFromRSocket(
      requester: RSocketRequester,
      submission: KeepAlivePackageSubmission
   ): Mono<SourceSubmissionResponse> {
      val rsocketId = connections.get(requester) ?: error("Unknown rsocket attempting to submit schemas")
      logger.info { "Received schema submission: $rsocketId with publisherId ${submission.sourcePackage.packageMetadata.identifier}" }
      taxiSchemaStoreWatcher.associateConnectionToPublisher(rsocketId, submission.publisherId)
      return Mono.just(submission)
         .publishOn(Schedulers.boundedElastic())
         .map {
            val result = submitKeepAlivePackage(submission).asSourceSubmissionResponse()
            val json = jacksonObjectMapper()
               .findAndRegisterModules()
               .writerWithDefaultPrettyPrinter()
               .writeValueAsString(result)
            result
         }
   }

   private fun handleSchemaPublisherDisconnect(rsocketId: TransportConnectionId) {
      val changeMessage =
         taxiSchemaStoreWatcher.markPackagesForTransportAsUnhealthy(rsocketId, "Publisher disconnected")
      // We're not removing schemas when publishers go offline anymore, just marking them as unhealthy.
//      if (changeMessage == null) {
//         logger.info { "RSocket $rsocketId disconnected, but no schemas were found to unpublish" }
//      } else {
//         logger.info { "RSocket $rsocketId disconnected, which generated ${changeMessage.deltas.size} deltas. Submitting now" }
//         validatingStore.submitUpdates(changeMessage)
//      }
      schemaUpdateNotifier.emitCurrentSchemaSet()
   }

   private fun submitKeepAlivePackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
      val sourcesUpdatedMessage = taxiSchemaStoreWatcher
         .submitSources(submission)
      return submitToValidatingStore(sourcesUpdatedMessage)
   }

   override fun submitPackage(submission: SourcePackage): Either<CompilationException, Schema> {
      logger.info { "Received Schema Submission From ${submission.packageMetadata.identifier} without keepalive data.  This will not be automaticatlly tidied up" }
      return submitKeepAlivePackage(
         KeepAlivePackageSubmission(submission)
      )

   }

   override fun submitMonitoredPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
   }

   override fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
   }

   private fun Either<CompilationException, Schema>.asSourceSubmissionResponse(): SourceSubmissionResponse {
      val either = this.map { validatingStore.schemaSet }
      return SourceSubmissionResponse.fromEither(either)
   }

}

