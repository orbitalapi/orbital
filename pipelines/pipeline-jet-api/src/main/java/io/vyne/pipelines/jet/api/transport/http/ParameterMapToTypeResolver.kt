package io.vyne.pipelines.jet.api.transport.http

import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.ParameterMap
import io.vyne.pipelines.jet.api.transport.PipelineLogger
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter


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
   fun resolveToTypes(parameterMap: ParameterMap, operation: Operation, logger: PipelineLogger = ConsoleLogger):Map<Parameter,Any> {
      return parameterMap.mapNotNull { (key,value) ->
         val matchedByName = operation.parameters
            .firstOrNull { it.isNamed(key) }
            ?.let { return@mapNotNull it to value }
         operation.parameters
            .firstOrNull { it.type.fullyQualifiedName == key }
            ?.let { return@mapNotNull it to value }
         logger.warn { "Operation ${operation.qualifiedName.fullyQualifiedName} does not contain a parameter that matches on key $key.  Attempts to match on either parameter name or type failed.  This parameter will be ignored" }
         null
      }.toMap()

   }
}
