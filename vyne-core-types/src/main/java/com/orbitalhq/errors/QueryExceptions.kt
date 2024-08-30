package com.orbitalhq.errors

import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.Schema

object QueryErrors {
   fun toException(error: TypedInstance, schema: Schema, responseHeaders: Map<String, List<String>>): OrbitalQueryException {
      // TODO ... here we should convert to the appropriate
      // OrbitalQueryException subtype defined below, falling back to a
      // OrbitalQueryException if none exist
      val errorMessage = when  {
          error is TypedObject -> {
             error.getAttributeIdentifiedByType(schema.type(ErrorType.ErrorMessageQualifiedName), returnNull = true)
                .value as String? ?: "A ${error.type.paramaterizedName} error was thrown"
          }
         error.type.isAssignableTo(schema.type(ErrorType.ErrorMessageQualifiedName)) -> error.value as String
         else -> throw IllegalArgumentException("Could not construct an error from provided type of ${error.type.paramaterizedName}")

      }
      return when (error.type.paramaterizedName) {
         else -> OrbitalQueryException(errorMessage, error, responseHeaders)
      }

   }
}
/**
 * base type for all taxi initiated errors
 * see: https://projects.notional.uk/youtrack/articles/ORB-A-27/Error-Handling
 */
open class OrbitalQueryException(message: String?, val error: TypedInstance, val responseHeaders: Map<String, List<String>>) : RuntimeException(message)
