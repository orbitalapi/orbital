package com.orbitalhq.nebula

import com.google.common.base.CaseFormat
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.config.ConfigSourceLoader
import com.orbitalhq.nebula.core.ComponentType
import com.orbitalhq.nebula.core.EnvVarKey
import com.orbitalhq.nebula.core.EnvVarValue
import com.orbitalhq.nebula.core.NebulaEnvVariablesMap
import com.orbitalhq.nebula.core.NebulaStackState
import com.orbitalhq.nebula.core.StackName
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemaServer.core.config.SchemaUpdateNotifier
import lang.taxi.utils.quoted
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

/**
 * A ConfigSourceLoader which exposes the settings from Nebula stack components
 * as environment variables, so they can be referenced inside HOCON files elsewhere
 */
@Component
class NebulaEnvVariableSource(
   private val schemaUpdateNotifier: SchemaChangedEventProvider
) : ConfigSourceLoader {
   private val variables = ConcurrentHashMap<String, String>()
   private val contentUpdatedSink = Sinks.many().multicast().directBestEffort<Class<out ConfigSourceLoader>>()

   override fun load(): List<SourcePackage> {
      val source = variables.entries.joinToString("\n") { (key, value) ->
         "${key.quoted()}: ${value.quoted()}"
      }
      val packageMetadata = PackageMetadata.from("nebula", "nebula-variables", "1.0.0")
      val sourcePackage = SourcePackage(
         packageMetadata,
         sources = listOf(
            VersionedSource("variables.conf", "1.0.0", source, packageMetadata.identifier)
         )
      )
      return listOf(sourcePackage)
   }

   fun updateEnvVariables(currentStackState: NebulaStackState): NebulaEnvVariablesMap {
      logger.info { "Updating environment variables defined in Nebula source package" }
      // A list of all the environment variables, grouped by each
      // component
      val envVariablesByComponent = getVariablesByStackComponent(currentStackState)
      variables.clear()
      variables.putAll(envVariablesByComponent.flatten())

      contentUpdatedSink.emitNext(NebulaEnvVariableSource::class.java, Sinks.EmitFailureHandler.FAIL_FAST)

      // Normally, the schema updates when files change.
      // But this update is entirely in-memory, so we
      // need to manually force a schema changed event
      schemaUpdateNotifier.forceSchemaChangedEvent()
      return envVariablesByComponent
   }


   override val contentUpdated: Flux<Class<out ConfigSourceLoader>>
      get() = contentUpdatedSink.asFlux()

   companion object {
      private val logger = KotlinLogging.logger {}

      /**
       * Extracts environment variables from the given Nebula stack state.
       *
       * For each entry in a ComponentInfo.config, multiple env variables are generated, in order
       * to allow users to use the most convenient env variable that isn't conflicting.
       *
       * eg: A stack named [com.test:hello-world]/services with a "kafka" entry containing "bootstrapServers",
       * would generate:
       *  - NEBULA_KAFKA_BOOTSTRAP_SERVERS <-- the most convenient, but least specific (highest chance of a conflict)
       *  - NEBULA_SERVICES_KAFKA_BOOTSTRAP_SERVERS
       *  - NEBULA_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS
       *  - NEBULA_COM_TEST_HELLO_WORLD_SERVICES_KAFKA_BOOTSTRAP_SERVERS <-- the least convenient, but most specific (lowest chance of a conflict)
       *
       *
       * @param stack The NebulaStackState object containing stack configurations.
       * @return A map where the keys are environment variable names and the values are the corresponding configuration values.
       */
      fun extractVariables(stack: NebulaStackState): Map<String, String> {
         val envVariablesByStack = getVariablesByStackComponent(stack)
         return envVariablesByStack.flatten()
      }

      fun getVariablesByStackComponent(stack: NebulaStackState): Map<StackName, Map<ComponentType, Map<EnvVarKey, EnvVarValue>>> {
         // Create a map of environment variables by iterating over each stack entry
         val environmentVariables = stack.map { (stackName, componentMap) ->
            // Generate possible stack name variations for environment variable prefixes
            val stackNameVariations = getStackNameVariations(stackName)
            // Flatten component configurations into key-value pairs with appropriate variable names
            val stackEnvironmentVariables = componentMap.map { componentInfoWithState  ->
               val componentInfo = componentInfoWithState.componentInfo
               val componentName =  componentInfoWithState.name
               val componentVariables: Map<String, String> = if (componentInfo != null) {
                   componentInfo.componentConfigMap.entries.flatMap { (key, value) ->
                     // Combine the stack name variations, component type, and key to form the env var names

                     stackNameVariations.map { prefix ->
                        listOf(prefix, toEnvVarConvention(componentName), toEnvVarConvention(key))
                           .filter { it.isNotEmpty() } // Remove any empty parts
                           .joinToString("_") to value.toString() // Create the env var name
                     }
                  }.toMap()
               } else emptyMap()

               componentName to componentVariables
            }.toMap()
            stackName to stackEnvironmentVariables
         }.toMap()
         return environmentVariables
      }

      /**
       * Generates variations of the stack name to be used as prefixes for environment variables.
       *
       * If the stack name is in the form "[orgId:projId]/fileName", it generates multiple variations:
       * - "NEBULA"
       * - "NEBULA_fileName"
       * - "NEBULA_projId_fileName"
       * - "NEBULA_orgId_projId_fileName"
       *
       * Otherwise, it handles stack names in the form "identifier/stackNamePart".
       *
       * @param stackName The original stack name.
       * @return A list of stack name variations formatted as environment variable prefixes.
       */
      private fun getStackNameVariations(stackName: String): List<String> {
         // Check if the stack name contains a package ID in the form [orgId:projId]/fileName
         val variations = if (stackName.contains("]")) {
            val (packageId, stackNamePart) = stackName.removePrefix("[").split("]")
            val (orgId, projectId) = packageId.split(":")
            val cleanedStackNamePart = stackNamePart.removePrefix("/")
            listOf(
               emptyList(),
               listOf(cleanedStackNamePart),
               listOf(projectId, cleanedStackNamePart),
               listOf(orgId, projectId, cleanedStackNamePart)
            )
         } else {
            // Handle the case where the stack name is in the form identifier/stackNamePart
            val (identifier, stackNamePart) = stackName.split("/")
            listOf(
               emptyList(),
               listOf(stackNamePart),
               listOf(identifier, stackNamePart),
            )
         }

         // Convert each variation into the expected env var convention format
         val names = variations.map { variationParts ->
            val variationPartsInConvention = variationParts.map { toEnvVarConvention(it) }
            (listOf("NEBULA") + variationPartsInConvention)
               .joinToString("_")
         }
         return names
      }

      /**
       * Converts an input string into an environment variable format (UPPER_UNDERSCORE).
       *
       * The conversion depends on the case of the first character in the input:
       * - Lowercase first character: Treated as lowerCamelCase.
       * - Uppercase first character: Treated as UpperCamelCase.
       *
       * Additionally, dots ('.') and dashes ('-') in the input are replaced with underscores ('_').
       *
       * @param input The string to convert.
       * @return The converted string in UPPER_UNDERSCORE format.
       */
      private fun toEnvVarConvention(input: String): String {
         // Determine the case format (camelCase vs PascalCase) and convert to UPPER_UNDERSCORE
         val caseFormat = if (input.first().isLowerCase()) {
            CaseFormat.LOWER_CAMEL
         } else CaseFormat.UPPER_CAMEL
         val converted = caseFormat.to(CaseFormat.UPPER_UNDERSCORE, input)
            .replace(".", "_") // Replace dots with underscores
            .replace("-", "_") // Replace dashes with underscores
         return converted
      }
   }
}

private fun Map<String, Map<String, Map<String, String>>>.flatten(): Map<String, String> {
   val result = mutableMapOf<String, String>()
   this.values.forEach { map ->
      map.values.forEach { innerMap ->
         result.putAll(innerMap)
      }
   }
   return result
}
