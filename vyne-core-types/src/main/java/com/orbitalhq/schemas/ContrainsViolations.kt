package com.orbitalhq.schemas

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.taxi.toVyneQualifiedName
import com.orbitalhq.utils.log
import com.orbitalhq.utils.orElse
import lang.taxi.expressions.Expression
import lang.taxi.services.operations.constraints.PropertyFieldNameIdentifier
import lang.taxi.services.operations.constraints.PropertyIdentifier
import lang.taxi.services.operations.constraints.PropertyTypeIdentifier
import mu.KotlinLogging

interface ConstraintViolation : ConstraintViolationValueUpdater {
   // TODO : This is cheating a bit.
   // While it _may_ be reasonable for a violation to have an opinion about
   // how it can be resolved (maybe), it shouldn't really know about service
   // contracts.  But, this will work for now.
   fun provideResolutionAdvice(operation: Operation, contract: OperationContract): ResolutionAdvice? = null

   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      val updated = updater.resolveWithUpdatedValue(updatedValue)
      return updated
   }

   val updater: ConstraintViolationValueUpdater
}

interface ConstraintViolationValueUpdater {
   fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance
}

/**
 * Returned in scenarios where we don't yet support updating values on constraints.
 * This doesn't mean that we shouldn't support the scneario, but that we haven't.
 * Note - we don't always NEED to support value updating.
 *
 * We need to support value updating when trying to find / resolve inputs for operations.
 * However, when evaluating a TypedInstance against a constraint, sometimes we just need to know that
 * it's invalid, rather than know how to update it.
 *
 * Basically, if you hit the exception thrown by this class, then we should add support.
 */
data class UnsupportedConstraintViolationValueUpdater(private val components: Set<Expression>, private val message: String) : ConstraintViolationValueUpdater {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   override fun resolveWithUpdatedValue(updatedValue: TypedInstance): TypedInstance {
      logger.error { message }
      // Throw an error, but only if someone actually tries to use this updater to actually update values.
      error(message)
   }
}

fun Parameter.matches(propertyIdentifier: PropertyIdentifier, matchOnNameOnly: Boolean = false): Boolean {
   return when (propertyIdentifier) {
      is PropertyFieldNameIdentifier -> propertyIdentifier.name.path == this.name
      is PropertyTypeIdentifier -> {
         !matchOnNameOnly &&
            this.type.isAssignableFrom(propertyIdentifier.type.toVyneQualifiedName())
      }

   }
}

data class ResolutionAdvice(val operation: Operation, val suggestedParams: Map<PropertyIdentifier, TypedInstance>) {
   fun containsValueForParam(parameter: Parameter): Boolean {

      // Relaxing this.
      // Originally, parameters must be named.
      // So check for an explicity named parameter first:
      if (suggestedParams.keys.any { parameter.matches(it, matchOnNameOnly = true) }) {
         return true
      }
      // However, if we can find an unambiguous match, use that.
      // Revert to named params only if this becomes problematic
      // Check to see if the type is unambiguous
      val matches = suggestedParams.keys.filter { parameter.matches(it) }
      if (matches.size > 1) {
         log().warn("Parameter ${parameter.name.orElse(parameter.type.fullyQualifiedName)} has multiple ambiguous matches, so none will match: ${matches.joinToString { it.description }}")
      }
      return matches.size == 1
   }

   fun getParamValue(parameter: Parameter): TypedInstance {
      val matchOnName = suggestedParams.filter { (suggestion, _) -> parameter.matches(suggestion, matchOnNameOnly = true) }
         .values
         .firstOrNull()
      if (matchOnName != null) {
         return matchOnName
      }

      return suggestedParams
         .filter { (suggestion, _) -> parameter.matches(suggestion) }
         .values
         .firstOrNull() ?: error("No match found, although one was expected.  This shouldn't happen")
   }
}
