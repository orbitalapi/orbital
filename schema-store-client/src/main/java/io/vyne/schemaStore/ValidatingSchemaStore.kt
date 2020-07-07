package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.ParsedSource
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaStore.hazelcast.SchemaSetCacheKey
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.utils.log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Schema store which upon having a schema submitted to it, will first
 * validate the schema compiles before updating the schemaset.
 */
class ValidatingSchemaStore(private val schemaValidator: SchemaValidator = TaxiSchemaValidator()) : SchemaStore, SchemaPublisher {
   private val generationCounter = AtomicInteger(0)
   private val schemaSetHolder = mutableMapOf<SchemaSetCacheKey, SchemaSet>()
   private val schemaSourcesMap = mutableMapOf<SchemaId, ParsedSource>()

   override fun schemaSet(): SchemaSet {
      TODO("Not yet implemented")
   }

   override val generation: Int
      get() {
         return generationCounter.get()
      }

   private val sources:List<ParsedSource>
   get() {
      return schemaSourcesMap.values.toList()
   }
   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      val validationResult = schemaValidator.validate(schemaSet(), versionedSources)
      val (parsedSources, returnValue) = when (validationResult) {
         is Either.Right -> {
            validationResult.b.second to Either.right(validationResult.b.first)
         }
         is Either.Left -> {
            validationResult.a.second to Either.left(validationResult.a.first)
         }
      }
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
         schemaSourcesMap[parsedSource.source.id] = parsedSource
      }
      rebuildAndStoreSchema()
      return returnValue
   }


   private fun rebuildAndStoreSchema(): SchemaSet {
      val result = SchemaSet.fromParsed(sources, generationCounter.incrementAndGet())
      log().info("Rebuilt schema cache - $result")
      schemaSetHolder.compute(SchemaSetCacheKey) { _, current ->
         when {
            current == null -> {
               log().info("Persisting first schema to cache: $result")
               result
            }
            current.generation >= result.generation -> {
               log().info("Not updating the cache for $result, as the current seems later. (Current: $current)")
               current
            }
            else -> {
               log().info("Updating schema cache with $result")
               result
            }
         }
      }
      return result
   }

}
