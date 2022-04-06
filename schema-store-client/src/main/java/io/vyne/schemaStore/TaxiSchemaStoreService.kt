package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.SourceSubmissionResponse
import io.vyne.schema.publisher.VersionedSourceSubmission
import io.vyne.schemas.Schema
import lang.taxi.CompilationError
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks

private val logger = KotlinLogging.logger { }

/**
 * Provides a central schema store where individual schema publishers push their schemas.
 * This Central Schema store works with io.vyne.schemaStore.HttpSchemaStoreClient. To see it in action:
 *  1. configure Schema Server with:
 *  --vyne.schema.publicationMethod=LOCAL
 *  2. Schema Publishers (e.g. vyne query server, cask etc.) with:
 *  --vyne.schema.publicationMethod=REMOTE
 * Each schema publication contains a list of versioned Sources and an identity of the publisher long with the heartbeat duration in seconds
 * Upon submission of versioned sources, this central schema store expects publisher to call listSchemas to fetch the updates.
 * If the publisher submits its schemas with a keep alive strategy, central schema store handles performing relevant keep alive actions.
 * If they miss heartbeating, relevant sources are removed from the schema store.
 *
 *      .----------------.   .----------------------.
 *      |Schema Publisher|   |TaxiSchemaStoreService|
 *      '----------------'   '----------------------'
 *      |                    |
 *      |  submitSources()   |
 *      |------------------->|
 *      |                    |
 *      |   listSchemas()    |
 *      |------------------->|
 *      |                    |
 *      |   ping()           |
 *      |<-------------------|
 *      |   ping()           |
 *      |<-------------------|
 *      |   ping()           |
 *      |<-------------------|
 *      |   ping()           |
 *      |<-------------------|
 *
 *             .
 *             .
 *             .
 *
 *
 */

@RestController
@RequestMapping("/api/schemas/taxi")
class TaxiSchemaStoreService(
    val keepAliveStrategyMonitors: List<KeepAliveStrategyMonitor>,
    private val validatingStore: LocalValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()) :
   SchemaSourceProvider, InitializingBean {
   // internal for testing purposes.
   internal val taxiSchemaStoreWatcher = ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors)

   @RequestMapping(method = [RequestMethod.POST])
   fun submitSources(@RequestBody submission: VersionedSourceSubmission): Mono<SourceSubmissionResponse> {
      val compilationResultSink = Sinks.one<Pair<SchemaSet, List<CompilationError>>>()
      val resultMono = compilationResultSink.asMono().cache()
      taxiSchemaStoreWatcher
         .submitSources(
            submission = submission,
            resultConsumer = { result: Pair<SchemaSet, List<CompilationError>> -> compilationResultSink.tryEmitValue(result) }
         )

      return resultMono.map { (schemaSet, errors) ->
         SourceSubmissionResponse(errors, schemaSet)
      }
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
      logger.info { "Initialised TaxiSchemaStoreService" }
      taxiSchemaStoreWatcher
         .currentSources
         .subscribe { currentState ->
            logger.info { "Received an update of SchemaSources, submitting to schema store" }
            val result = validatingStore.submitSchemas(currentState.sources, currentState.removedSchemaIds)
            currentState.resultConsumer?.let {
               val errorList = if (result is Either.Left) result.a.errors else emptyList()
               it(Pair(validatingStore.schemaSet(), errorList))
            }
         }
   }
}
