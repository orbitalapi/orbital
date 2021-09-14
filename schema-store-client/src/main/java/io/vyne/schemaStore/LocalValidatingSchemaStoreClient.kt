package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.ParsedSource
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.utils.log
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * Schema store which upon having a schema submitted to it, will first
 * validate the schema compiles before updating the schemaset.
 *
 * assume the following:

In schema server

foo.taxi:

model Foo {
@Between
time: DateTime
}

Assume Cask is already created for Foo.


t0:

Eureka, Vyne, Schema Server, Cask up

schema server ->   foo.taxi version 0.1.0
Cask -> vyne.cask.Foo  version 1.0.1

service vyne.cask.Foo {

operation findByTimeBetween...
}


t1:

foo.taxi is modified as:

model Foo {
@Between
time: DateTime
@Between
date: Date
}

schema server ->   foo.taxi version 0.2.0
Cask -> vyne.cask.Foo  version 1.0.2

service vyne.cask.Foo {

operation findByTimeBetween...
operation findByDateBetween...
}

t2:


foo.taxi is modified as:

model Foo {
@Between
time: DateTime
}


and schema server immediately bounced.

schema server ->   foo.taxi version 0.1.0
Cask receives taxi.version 0.1.0 from Vyne
it doesn't update its own taxi.version 0.2.0 as 0.2.0 > 0.1.0

and keep cask definition as:

service vyne.cask.Foo {

operation findByTimeBetween...
operation findByDateBetween...
}

 */
class LocalValidatingSchemaStoreClient(private val schemaValidator: SchemaValidator = TaxiSchemaValidator()) :
   SchemaStoreClient {
   private val generationCounter = AtomicInteger(0)
   private val schemaSetHolder = ConcurrentHashMap<SchemaSetCacheKey, SchemaSet>()
   private val schemaSourcesMap = ConcurrentHashMap<String, ParsedSource>()

   override fun schemaSet(): SchemaSet {
      return schemaSetHolder[SchemaSetCacheKey] ?: SchemaSet.EMPTY
   }

   override val generation: Int
      get() {
         return generationCounter.get()
      }

   private val sources: List<ParsedSource>
      get() {
         return schemaSourcesMap.values.toList()
      }

   fun removeSources(schemaIds: List<String>) {
      schemaIds.forEach { this.schemaSourcesMap.remove(it) }
   }

   fun removeSourceAndRecompile(schemaIds: List<String>) {
      removeSources(schemaIds)
      rebuildAndStoreSchema()
   }

   fun removeSourceAndRecompile(schemaId: SchemaId) {
      this.removeSourceAndRecompile(listOf(schemaId))
   }

   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      logger.info { "Initiating change to schemas, currently on generation ${this.generation}" }
      logger.info { "Submitting the following schemas: ${versionedSources.joinToString { it.id }}" }
      logger.info { "Removing the following schemas: ${removedSources.joinToString { it }}" }
      val (parsedSources, returnValue) = schemaValidator.validateAndParse(schemaSet(), versionedSources, removedSources)
      parsedSources.forEach { parsedSource ->
         // TODO : We now allow storing schemas that have errors.
         // This is because if schemas depend on other schemas that go away, (ie., from a service
         // that goes down).
         // we want them to become valid when the other schema returns, and not have to have the
         // publisher re-register.
         // Also, thi`s is useful for UI tooling.
         // However, by overwriting the source in the cache using the id, there's a small
         // chance that if publishers aren't incrementing their ids properly, that we
         // overwrite a valid source with on that contains compilation errors.
         // Deal with that if the scenario arises.
         schemaSourcesMap[parsedSource.source.name] = parsedSource
      }
      removedSources.forEach { schemaIdToRemove ->
         val (name, _) = VersionedSource.nameAndVersionFromId(schemaIdToRemove)
         val removed = schemaSourcesMap.remove(name)
         if (removed == null) {
            logger.warn { "Failed to remove source with schemaId $schemaIdToRemove as it was not found in the collection of sources" }
         }
      }
      rebuildAndStoreSchema()
      logger.info { "After schema update operation, now on generation $generation" }
      return returnValue.mapLeft { CompilationException(it) }
   }


   private fun rebuildAndStoreSchema(): SchemaSet {
      val result = synchronized(this) {
         val parsedResult = SchemaSet.fromParsed(sources, generationCounter.incrementAndGet())
         log().info("Rebuilt schema cache - $parsedResult")
         schemaSetHolder.compute(SchemaSetCacheKey) { _, current ->
            when {
               current == null -> {
                  log().info("Persisting first schema to cache: $parsedResult")
                  parsedResult
               }
               current.generation >= parsedResult.generation -> {
                  log().info("Not updating the cache for $parsedResult, as the current seems later. (Current: $current)")
                  current
               }
               else -> {
                  log().info("Updating schema cache with $parsedResult")
                  // Eagerly compute the schema, so we do it at schema update time, rather than
                  // query time.
                  parsedResult.schema
                  parsedResult
               }
            }
         }
         parsedResult
      }
      return result
   }

}
