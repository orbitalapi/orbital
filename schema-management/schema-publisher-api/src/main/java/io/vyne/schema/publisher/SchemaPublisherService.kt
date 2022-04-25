package io.vyne.schema.publisher

import io.vyne.VersionedSource
import io.vyne.schema.publisher.loaders.SchemaSourcesLoader
import io.vyne.schemas.taxi.toMessage
import lang.taxi.generators.GeneratedTaxiCode
import mu.KotlinLogging
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * The entry point for schema publication
 * Responsible for pushing schemas to the schema server, using a configured
 * data transport.
 */
class SchemaPublisherService(
   private val publisherId: String,
   private val transport: SchemaPublisherTransport
) {
   private val logger = KotlinLogging.logger {}

   private fun defaultConverter(code: GeneratedTaxiCode, index: Int): VersionedSource {
      return VersionedSource.sourceOnly(code.concatenatedSource)
   }

   /**
    * Simple adaptor for converting from Taxi's GeneratedCode artifacts
    * to Vyne's VersionedSource.
    *
    * Optionally allows a dedicated converter to be provided
    */
   fun publish(
      sources: List<GeneratedTaxiCode>,
      converter: (GeneratedTaxiCode, Int) -> VersionedSource = ::defaultConverter
   ): Flux<SourceSubmissionResponse> {
      val versionedSources = sources.mapIndexed { index, source -> converter(source, index) }
      return publish(versionedSources)
   }

   fun loadAndPublish(
      sourcesLoader: SchemaSourcesLoader
   ): Flux<SourceSubmissionResponse> {
      return loadAndPublish(listOf(sourcesLoader))
   }

   fun loadAndPublish(
      sourcesLoaders: List<SchemaSourcesLoader>
   ): Flux<SourceSubmissionResponse> {
      val sources = sourcesLoaders.flatMap { it.load() }
      return publish(sources)
   }


   private val responsesSink = Sinks.many().multicast().directBestEffort<SourceSubmissionResponse>()
   val responses: Flux<SourceSubmissionResponse>
      get() {
         return responsesSink.asFlux()
      }

   private var activeSubmissionSubscription: Disposable? = null

   private fun unsubscribeFromSchemaSubmission() {
      activeSubmissionSubscription?.let {
         it.dispose()
         activeSubmissionSubscription = null
      }
   }

   /**
    * Submits the provided sources.
    *
    * Depending on the transport and it's ability to recover from
    * reconnects etc, sources can be submitted multiple times, when events such as disconnect / reconnect
    * occur.  Therefore, the response is a Flux<>.
    *
    */
   fun publish(sources: List<VersionedSource>): Flux<SourceSubmissionResponse> {
      return if (transport is AsyncSchemaPublisherTransport) {
         unsubscribeFromSchemaSubmission()
         activeSubmissionSubscription = transport.submitSchemaOnConnection(
            publisherId,
            sources
         ).subscribe { result ->
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

         // Return the outer flux, rather than the direct subscription.
         // This allows consumers of this class to have a single point
         // to subscribe to that reliably emits schema changes.
         // Otherwise, the Flux<> for subscription is constantly changing.
         responsesSink.asFlux()
      } else {
         // What's the contract in traditional sources like HTTP?
         // Whos responsible for doing things like recovery when the connection is re-established?
         TODO("How does this actually work in things like HTTP?")
//         val result = transport.submitSchemas(sources)
//         when (result) {
//            is Either.Left -> logger.warn { "Schema submission failed.  The following errors were returned: \n${result.a.errors.toMessage()}" }
//            is Either.Right -> logger.info { "Schema submitted successfully" }
//         }
      }

   }
}
