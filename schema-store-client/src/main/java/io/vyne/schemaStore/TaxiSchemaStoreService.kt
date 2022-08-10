package io.vyne.schemaStore

import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.ExpiringSourcesStore
import io.vyne.schema.publisher.KeepAlivePackageSubmission
import io.vyne.schema.publisher.KeepAliveStrategyMonitor
import io.vyne.schema.publisher.SourceSubmissionResponse
import mu.KotlinLogging
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

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
   private val validatingStore: LocalValidatingSchemaStoreClient = LocalValidatingSchemaStoreClient()
) :
   SchemaSourceProvider {
   // internal for testing purposes.
   internal val taxiSchemaStoreWatcher = ExpiringSourcesStore(keepAliveStrategyMonitors = keepAliveStrategyMonitors)

   init {
      logger.info { "Initialised TaxiSchemaStoreService" }
      taxiSchemaStoreWatcher
         .currentSources
         .subscribe { update ->
            logger.info { "Received an update of SchemaSources, submitting to schema store" }
            val result = validatingStore.submitUpdates(update)
         }
   }

   @RequestMapping(method = [RequestMethod.POST])
   fun submitSources(@RequestBody submission: KeepAlivePackageSubmission): Mono<SourceSubmissionResponse> {
      val updateMessage = taxiSchemaStoreWatcher
         .submitSources(submission)
      val result = validatingStore.submitUpdates(updateMessage)
         .map { validatingStore.schemaSet }
      return Mono.just(SourceSubmissionResponse.fromEither(result))
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


}
