package com.orbitalhq.schemaServer.core.config

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedPackageIdentifier
import com.orbitalhq.config.ConfigSourceErrorMessage
import com.orbitalhq.config.MergingHoconConfigRepository
import com.orbitalhq.utils.flattenMaps

/**
 * Observes the health from MergingHoconConfigRepositories,
 * and advises when there's issues (eg., unable to resolve variables, etc)
 */
class ConfigHealthMonitor(
   val repositories: List<MergingHoconConfigRepository<*>>
) {
   fun repositoriesWithErrors(): Map<VersionedPackageIdentifier, Map<String, ConfigSourceErrorMessage>> {
      val result = mutableMapOf<VersionedPackageIdentifier, MutableMap<String, ConfigSourceErrorMessage>>()
      repositories.forEach { configRepo ->
         val configSourcesWithErrors = configRepo.configSources.filter { configSource -> configSource.hasError }
            .groupBy { it.packageIdentifier.id }
            .mapValues { (_, configSources) ->
               configSources.mapNotNull {
                  (it.configSourceName ?: "Unknown source") to (it.error ?: "Unknown Error")
               }
            }
         configSourcesWithErrors.forEach { (packageId, errors) ->
            val errorsForPackage = result.getOrPut(packageId) { mutableMapOf() }
            errorsForPackage.putAll(errors.toMap())
         }
      }
      return result
   }

   fun getConfigurationErrors(packageIdentifier: PackageIdentifier): Map<String, ConfigSourceErrorMessage>  {
      return repositoriesWithErrors().getOrDefault(packageIdentifier.id, emptyMap())
   }
}
