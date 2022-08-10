package io.vyne.schema.publisher

import arrow.core.Either
import io.vyne.PackageMetadata
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaSet
import io.vyne.schemas.taxi.toMessage
import lang.taxi.generators.GeneratedTaxiCode
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * The entry point for schema publication
 *
 * Responsible for pushing schemas to the schema server, using a configured
 * data transport.
 *
 *
 */
class SchemaPublisherService(
   private val publisherId: String,
   private val transport: SchemaPublisherTransport
) {

   private val logger = KotlinLogging.logger {}

   private fun defaultConverter(code: GeneratedTaxiCode, index: Int): VersionedSource {
      return VersionedSource.unversioned(publisherId, code.concatenatedSource)
   }

   /**
    * Simple adaptor for converting from Taxi's GeneratedCode artifacts
    * to Vyne's VersionedSource.
    *
    * Optionally allows a dedicated converter to be provided.
    * Allows for publication of a different type of source (eg.,
    * OpenApi spec), which is transpiled client-side
    */
   fun publish(
      packageMetadata: PackageMetadata,
      sources: List<GeneratedTaxiCode>,
      converter: (GeneratedTaxiCode, Int) -> VersionedSource = ::defaultConverter
   ): Flux<SourceSubmissionResponse> {
      val versionedSources = sources.mapIndexed { index, source -> converter(source, index) }
      return publish(SourcePackage(packageMetadata, versionedSources))
   }

//   fun loadAndPublish(
//      sourcesLoader: SchemaSourcesLoader
//   ): Flux<SourceSubmissionResponse> {
//      return loadAndPublish(listOf(sourcesLoader))
//   }
//
//   fun loadAndPublish(
//      sourcesLoaders: List<SchemaSourcesLoader>
//   ): Flux<SourceSubmissionResponse> {
//      val sources = sourcesLoaders.flatMap { it.load() }
//      return publish(sources)
//   }


   private val responsesSink = Sinks.many().multicast().directBestEffort<SourceSubmissionResponse>()
   val responses: Flux<SourceSubmissionResponse>
      get() {
         return responsesSink.asFlux()
      }

   private var transportSubmissionsResponseSubscription: Disposable? = null

   private var currentSourcesSubmissionSubscription: Disposable? = null


   /**
    * Submits the provided sources.
    *
    * Depending on the transport and it's ability to recover from
    * reconnects etc, sources can be submitted multiple times, when events such as disconnect / reconnect
    * occur.  Therefore, the response is a Flux<>.
    *
    */
   fun publish(
      submission: SourcePackage,
   ): Flux<SourceSubmissionResponse> {
      return if (transport is AsyncSchemaPublisherTransport) {
         subscribeOnceAndRebroadcast(transport.sourceSubmissionResponses)

         // If we were already publishing sources, stop, as they're now out of date
         currentSourcesSubmissionSubscription?.let { subscription ->
            logger.info { "Stopping existing publication of sources, as new sources have been provided" }
            subscription.dispose()
            currentSourcesSubmissionSubscription = null

         }

         // We subscribe here, and hold the subscription.
         // The transport layer keep publishing this on all new rsocket connections,
         // to correctly recover from a disconnect / reconnect.
         currentSourcesSubmissionSubscription = transport.submitSchemaOnConnection(
            transport.buildKeepAlivePackage(submission, publisherId)
         ).subscribe {
            responsesSink.emitNext(it) { signalType, emitResult ->
               logger.warn { "Receved a source submission response, but failed to emit it on our internal responsesSink: $signalType $emitResult" }
               true
            }
         }

         // Return the outer flux, rather than the direct subscription.
         // This allows consumers of this class to have a single point
         // to subscribe to that reliably emits schema changes.
         // Otherwise, the Flux<> for subscription is constantly changing.
         responsesSink.asFlux()
      } else {
         // What's the contract in traditional sources like HTTP?
         // Who's responsible for doing things like recovery when the connection is re-established?
         val result = transport.submitPackage(submission)
            // Not sure about what to use for the generation here.
            .map { schema -> SchemaSet.from(schema, 0) }
         when (result) {
            is Either.Left -> logger.warn { "Schema submission failed.  The following errors were returned: \n${result.a.errors.toMessage()}" }
            is Either.Right -> logger.info { "Schema submitted successfully" }
         }
         Flux.just(SourceSubmissionResponse.fromEither(result))

      }

   }

   /**
    * Subscribes on the sourceSubmissionResponses flux once,
    * and emits responses back on our own internal flux.
    *
    * This keeps consumers of the SchemaPublisherService
    * unaware of changing fluxes when reconnections occur.
    */
   private fun subscribeOnceAndRebroadcast(sourceSubmissionResponses: Flux<SourceSubmissionResponse>) {
      if (transportSubmissionsResponseSubscription == null) {
         transportSubmissionsResponseSubscription = sourceSubmissionResponses.subscribe { result ->
            if (result.isValid) {
               logger.info { "Schema submitted successfully, now on generation ${result.schemaSet.generation}" }
            } else {
               logger.warn { "Schema submission failed.  The following errors were returned: \n${result.errors.toMessage()}" }
            }
            responsesSink.emitNext(result) { signalType, emitResult ->
               logger.warn { "Failed to emit result from schema submission: $signalType $emitResult" }
               true
            }
         }
      }
   }
}
