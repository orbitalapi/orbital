package io.vyne.schemaStore

import arrow.core.Either
import arrow.core.right
import io.vyne.*
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaValidator
import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.publisher.*
import io.vyne.schemas.Schema
import lang.taxi.CompilationError
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationException
import lang.taxi.utils.log
import mu.KotlinLogging
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
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
class LocalValidatingSchemaStoreClient : ValidatingSchemaStoreClient(
   schemaSetHolder = ConcurrentHashMap(),
   packagesById = ConcurrentHashMap()
) {
   private val generationCounter = AtomicInteger(0)
   override fun incrementGenerationCounterAndGet(): Int = generationCounter.incrementAndGet()
   override val generation: Int
      get() {
         return generationCounter.get()
      }


}

object SchemaSetCacheKey : Serializable

abstract class ValidatingSchemaStoreClient(
   private val schemaValidator: SchemaValidator = TaxiSchemaValidator(),
   protected val schemaSetHolder: ConcurrentMap<SchemaSetCacheKey, SchemaSet>,
   protected val packagesById: ConcurrentMap<UnversionedPackageIdentifier, ParsedPackage>
) : SchemaSetChangedEventRepository(), SchemaStore, SchemaPublisherTransport {
   override val schemaSet: SchemaSet
      get() {
         return schemaSetHolder[SchemaSetCacheKey] ?: SchemaSet.EMPTY
      }

   private val packages: List<ParsedPackage>
      get() {
         return packagesById.values.toList()
      }

   var lastSubmissionResult: Either<CompilationException, Schema> = Either.right(TaxiSchema.empty())
      private set

   private val sources: List<ParsedSource>
      get() {
         return packagesById.values.toList().flatMap { it.sources }
      }

   var lastCompilationMessages: List<CompilationError> = emptyList()
      private set;


   fun submitUpdates(message: PackagesUpdatedMessage): Either<CompilationException, Schema> {
      val submissionResults = message.deltas.mapNotNull { delta ->
         when (delta) {
            is PackageAdded -> submitPackage(delta.newState)
            is PackageUpdated -> submitPackage(delta.newState)
            is PackageRemoved -> removeSchemas(listOf(delta.oldStateId))
            is PublisherHealthUpdated -> null
         }
      }
      return if (submissionResults.isEmpty()) {
         schemaSet.schema.right()
      } else {
         submissionResults.last()
      }
   }

   override fun submitMonitoredPackage(submission: KeepAlivePackageSubmission): Either<CompilationException, Schema> {
      TODO("Not yet implemented")
   }

   override fun submitPackage(submission: SourcePackage): Either<CompilationException, Schema> {
      logger.info { "Received schema submission ${submission.identifier}" }
      return submitChanges(submission, emptyList())
   }

   override fun removeSchemas(identifiers: List<PackageIdentifier>): Either<CompilationException, Schema> {
      return submitChanges(null, identifiers)
   }

   private fun submitChanges(
      updatedPackage: SourcePackage?,
      removedPackages: List<PackageIdentifier>
   ): Either<CompilationException, Schema> {
      logger.info { "Initiating change to schemas, currently on generation ${this.generation}" }
      if (updatedPackage != null) {
         logger.info { "Submitting the following schemas: ${updatedPackage.packageMetadata.identifier}" }
      }
      if (removedPackages.isNotEmpty()) {
         logger.info { "Removing the following schemas: ${removedPackages.joinToString { it.id }}" }
      }

      val (parsedPackages, returnValue) = schemaValidator.validateAndParse(schemaSet, updatedPackage, removedPackages)
      parsedPackages.forEach { parsedPackage ->
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
         packagesById[parsedPackage.identifier.unversionedId] = parsedPackage
      }
      removedPackages.forEach { schemaIdToRemove ->
//         val (name, _) = VersionedSource.nameAndVersionFromId(schemaIdToRemove)
         val packageWithUnversionedId = packagesById[schemaIdToRemove.unversionedId]
         // Not sure if not removing is the right play here.
         when {
             packageWithUnversionedId == null -> logger.warn { "Failed to remove source with schemaId $schemaIdToRemove as it was not found in the collection of sources" }
             packageWithUnversionedId.identifier != schemaIdToRemove -> logger.warn { "Conflict in schema version to remove for package ${schemaIdToRemove.unversionedId}.  Was asked to remove version ${schemaIdToRemove.version}, but version ${packageWithUnversionedId.identifier.version} is currently stored.  Not removing." }
             else -> packagesById.remove(schemaIdToRemove.unversionedId)
         }
      }
      rebuildAndStoreSchema()
      logger.info { "After schema update operation, now on generation $generation" }
      lastSubmissionResult = returnValue.mapLeft { CompilationException(it) }

      // For now, we're only storing compilation errors when there's a failure.
      // This means linter messages, warnings etc., are lost.
      // This will change in future.
      lastCompilationMessages = when (returnValue) {
         is Either.Left -> returnValue.a
         else -> emptyList()
      }

      return returnValue.mapLeft { CompilationException(it) }
   }

   protected abstract fun incrementGenerationCounterAndGet(): Int

   private fun rebuildAndStoreSchema(): SchemaSet {
      val result = synchronized(this) {
         val parsedResult = SchemaSet.fromParsed(packages, incrementGenerationCounterAndGet())
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
