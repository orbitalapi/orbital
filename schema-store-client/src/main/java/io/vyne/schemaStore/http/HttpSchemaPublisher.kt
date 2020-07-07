package io.vyne.schemaStore.http

import arrow.core.Either
import io.vyne.VersionedSource
import io.vyne.schemaStore.*
import io.vyne.schemas.Schema
import io.vyne.utils.log
import lang.taxi.CompilationException
import org.springframework.retry.RetryContext
import org.springframework.retry.support.RetryTemplate

/**
 * A simple schema publisher which defers all compilation and validation
 * to the remote schema store
 */
class HttpSchemaPublisher(
   private val schemaService: SchemaService,
   private val retryTemplate: RetryTemplate = RetryConfig.simpleRetryWithBackoff(),
   private val schemaStore: SimpleSchemaStore
) : SchemaPublisher {

   override fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> {
      return retryTemplate.execute<Either<CompilationException, Schema>, Exception> { context: RetryContext ->
         context.setAttribute(RetryConfig.RETRYABLE_PROCESS_NAME, "Publish schemas")
         log().debug("Submitting ${versionedSources.size} sources")
         val response = schemaService.submitSources(versionedSources)

         if (response.isValid) {
            schemaStore.setSchemaSet(response.schemaSet)
            log().info("Submitted sources successfully, now at schemaSet ${schemaStore.schemaSet()}")
            Either.right(schemaStore.schemaSet().schema)
         } else {
            val errors = response.errors.joinToString("\n") { it.detailMessage }
            log().error("Source submission was rejected because of compilation errors: \n$errors")
            Either.left(CompilationException(response.errors))
         }
      }
   }
}
