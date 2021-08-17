package io.vyne.pipelines.runner.transport

import io.vyne.models.TypedInstance
import io.vyne.pipelines.runner.transport.PipelineVariableKeys.isVariableKey
import io.vyne.pipelines.runner.transport.http.ParameterMapToTypeResolver
import io.vyne.schemas.Operation
import io.vyne.schemas.Parameter
import io.vyne.schemas.Schema
import mu.KotlinLogging
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

interface PipelineAwareVariableProvider {
   fun getVariableProvider(pipelineName: String): MutableVariableProvider

   companion object {
      fun default(
         state: MutableMap<String, MutableMap<String, Any>> = mutableMapOf(),
         clock: Clock = Clock.systemUTC(),
         variableSource: VariableSource = CompositeVariableSource.withDefaults(
            clock = clock
         )
      ): PipelineAwareVariableProvider {
         return DefaultPipelineAwareVariableProvider(
            state,
            variableSource,
            clock
         )
      }
   }
}

class DefaultPipelineAwareVariableProvider(
   private val pipelineState: MutableMap<String, MutableMap<String, Any>>,
   private val variableSource: VariableSource,
   private val clock: Clock = Clock.systemUTC(),
) : PipelineAwareVariableProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private fun newDefaultPipelineState(): Map<String, Any> {
      return mapOf(
         PipelineVariableKeys.PIPELINE_LAST_RUN_TIME to clock.instant()
      )
   }

   override fun getVariableProvider(pipelineName: String): MutableVariableProvider {
      val state = pipelineState.getOrPut(pipelineName) {
         logger.info { "No state available for pipeline $pipelineName, initiating default state" }
         mutableMapOf()
      }
      val pipelineDefaults = newDefaultPipelineState()
      val variableSourceWithDefaultFallback = CompositeVariableSource.withDefaults(
         listOf(
            variableSource,
            StaticVariableSource(
               pipelineDefaults,
               name = "pipelineDefaults"
            ) // Defaults come after the existing sources, so are used only as fallbacks
         ),
         clock
      )
      return MutableCompositeVariableProvider(
         state = state,
         variableSource = variableSourceWithDefaultFallback,
         name = pipelineName,
         clock = clock
      )
   }

}

class MutableCompositeVariableProvider(
   private val state: MutableMap<String, Any> = mutableMapOf(),
   val variableSource: VariableSource,
   private val name: String = "Unnamed Variable Provider",
   clock: Clock = Clock.systemUTC()
) : MutableVariableProvider {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val variableProvider = VariableProvider.default(
      listOf(
         StaticVariableSource(state),
         variableSource
      ),
      clock
   )

   override fun asTypedInstances(parameterMap: ParameterMap, operation: Operation, schema: Schema): List<Pair<Parameter,TypedInstance>> =
      variableProvider.asTypedInstances(parameterMap, operation, schema)

   override fun populate(parameterMap: ParameterMap): ParameterMap = variableProvider.populate(parameterMap)

   override fun set(key: String, value: Any) {
      logger.info { "Updating $key for variable provider $name to $value" }
      state.put(key, value);
   }


}

interface MutableVariableProvider : VariableProvider {
   fun set(key: String, value: Any)
}

/**
 * Responsible for swapping out variables in a map with their corresponding values.
 */
interface VariableProvider {
   fun populate(parameterMap: ParameterMap): ParameterMap
   fun asTypedInstances(parameterMap: ParameterMap, operation: Operation, schema: Schema): List<Pair<Parameter,TypedInstance>>

   companion object {
      fun empty(): VariableProvider {
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

   override fun asTypedInstances(parameterMap: ParameterMap, operation: Operation, schema: Schema): List<Pair<Parameter,TypedInstance>> {
      val populatedParameters = populate(parameterMap)
      return ParameterMapToTypeResolver.resolveToTypes(populatedParameters, operation)
         .map { (parameter, value) ->
            parameter to TypedInstance.from(parameter.type, value, schema)
         }

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

class StaticVariableSource(private val variables: Map<String, Any>, private val name: String = "unnamed") :
   VariableSource {
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
