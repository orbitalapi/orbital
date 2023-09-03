package com.orbitalhq.pipelines.jet.api.transport.http

import com.orbitalhq.pipelines.jet.api.transport.ParameterMap
import com.orbitalhq.schemas.Operation
import com.orbitalhq.schemas.Parameter
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object ParameterMapToTypeResolver {
   /**
    * Takes a ParameterMap - who's key could be either the parameter name or the parameter's type name
    * and resolves all keys to the correct type.
    *
    * The following resolution logic is used:
    *  - If a parameter exists on the operation with the name provided, resolve by name
    *  - Otherwise, resolve as a type
    *
    *  If neither match, then a warning is logged, and the parameter is excluded
    */
   fun resolveToTypes(parameterMap: ParameterMap, operation: Operation): Map<Parameter, Any> {
      return parameterMap.mapNotNull { (key, value) ->

         val matchedByName = operation.parameters
            .firstOrNull { it.isNamed(key) }
         if (matchedByName != null) {
            return@mapNotNull matchedByName to value
         }
         operation.parameters
            .firstOrNull { it.type.fullyQualifiedName == key }
            ?.let { return@mapNotNull it to value }
         logger.warn { "Operation ${operation.qualifiedName.fullyQualifiedName} does not contain a parameter that matches on key $key. Attempts to match on either parameter name or type failed. This parameter will be ignored." }
         null
      }.toMap()

   }
}