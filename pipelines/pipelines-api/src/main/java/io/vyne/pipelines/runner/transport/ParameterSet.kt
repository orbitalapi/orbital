package io.vyne.pipelines.runner.transport

import io.vyne.models.TypedInstance
import io.vyne.pipelines.runner.transport.PipelineVariableKeys.isVariableKey
import io.vyne.schemas.Schema
import java.time.Clock
import java.time.Instant

/**
 * A ParameterMap can be passed into a pipeline spec.
 * The key is a type name to be used.
 * The value is either a literal value, or a VariableKey.
 */
typealias ParameterMap = Map<String, Any>


/**
 * A set of variable keys that we provide values for.
 * Other variables may be populated elsewhere, such as auth tokens, etc
 */
object PipelineVariableKeys {
   const val PIPELINE_LAST_RUN_TIME = "\$pipeline.lastRunTime"

   const val ENV_CURRENT_TIME = "\$env.now"

   fun isVariableKey(name: Any): Boolean {
      return name is String && name.startsWith('$')
   }
}


/**
 * Responsible for swapping out variables in a map with their corresponding values.
 */
interface VariableProvider {
   fun populate(parameterMap: ParameterMap): ParameterMap
   fun asTypedInstances(parameterMap: ParameterMap, schema: Schema): Set<TypedInstance>

   companion object {
      fun empty():VariableProvider {
         return DefaultVariableProvider(StaticVariableSource(emptyMap()))
      }
      fun default(
         otherSources: List<VariableSource> = emptyList(),
         clock: Clock = Clock.systemUTC()
      ): VariableProvider {
         return DefaultVariableProvider(CompositeVariableSource.withDefaults(otherSources, clock))
      }

      fun defaultWith(otherValues: Map<String, Any>, clock: Clock = Clock.systemUTC()): VariableProvider {
         return default(
            listOf(StaticVariableSource(otherValues)),
            clock
         )
      }
   }
}

class DefaultVariableProvider(private val source: VariableSource) : VariableProvider {
   override fun populate(parameterMap: ParameterMap): ParameterMap {
      return parameterMap.map { (typeName, value) ->
         val populatedValue = if (isVariableKey(value) && source.canPopulate(value as String)) {
            source.populate(value as String)
         } else {
            value
         }
         typeName to populatedValue
      }.toMap()
   }

   override fun asTypedInstances(parameterMap: ParameterMap, schema: Schema): Set<TypedInstance> {
      val populatedParameters = populate(parameterMap)
      return populatedParameters.map { (typeName, value) ->
         TypedInstance.from(schema.type(typeName), value, schema)
      }.toSet()

   }

}

/**
 * A single specific source of variables.
 * These can come from many places, such as the physical machine,
 * or the upstream pipeline orchestrator state for this job, etc
 */
interface VariableSource {
   fun canPopulate(variableName: String): Boolean
   fun populate(variableName: String): Any
}

class CompositeVariableSource(private val sources: List<VariableSource>) : VariableSource {
   override fun canPopulate(variableName: String): Boolean = sources.any { it.canPopulate(variableName) }

   override fun populate(variableName: String): Any {
      return sources.first { it.canPopulate(variableName) }
         .populate(variableName)
   }

   companion object {
      fun withDefaults(
         otherSources: List<VariableSource> = emptyList(),
         clock: Clock = Clock.systemUTC()
      ): VariableSource {
         return CompositeVariableSource(listOf(EnvVariableSource(clock)) + otherSources)
      }
   }
}

class StaticVariableSource(private val variables: Map<String, Any>) : VariableSource {
   override fun canPopulate(variableName: String): Boolean = variables.containsKey(variableName)

   override fun populate(variableName: String): Any {
      return variables.getOrElse(variableName) { error("$variableName is not present in this source") }
   }

}

class EnvVariableSource(private val clock: Clock = Clock.systemUTC()) : VariableSource {
   private val variables: Map<String, () -> Any> = mapOf(
      PipelineVariableKeys.ENV_CURRENT_TIME to { Instant.now(clock) }
   )

   override fun canPopulate(variableName: String) = variables.containsKey(variableName)

   override fun populate(variableName: String): Any {
      val variableProvider = variables.getOrElse(variableName) { error("$variableName is not present in this source") }
      return variableProvider.invoke()

   }
}
