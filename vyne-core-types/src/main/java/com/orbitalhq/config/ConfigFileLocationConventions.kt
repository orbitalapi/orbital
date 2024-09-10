package com.orbitalhq.config

import lang.taxi.packages.SourcesType

/**
 * Provides additionalSources section settings in taxi.conf files.
 *
 * Allows white-label platforms to override config keys.
 *
 * Be very careful - these are global, shared state.
 * Changing these will impact several things throughout the platform, so
 * only change on application startup.
 *
 * If modifying in tests, be sure to call resetToDefaults() afterwards
 */
object ConfigFileLocationConventions {
   const val DEFAULT_CONFIG_KEY = "@orbital/config"
   const val DEFAULT_CONFIG_PATH = "orbital/config/*.conf"

   fun resetToDefaults() {
      OrbitalConfigKey = DEFAULT_CONFIG_KEY
      OrbitalConfigPathEntry = DEFAULT_CONFIG_PATH
   }

   var OrbitalConfigKey = DEFAULT_CONFIG_KEY
   var OrbitalConfigPathEntry = DEFAULT_CONFIG_PATH

   var OrbitalNebulaKey = "@orbital/nebula"
   var OrbitalNebulaPathEntry = "orbital/nebula/*.nebula.kts"

   fun getConventionalPathEntry(sourcesType: SourcesType): String {
      return conventions()[sourcesType] ?: error("No convention exists for additional sources type of $sourcesType")
   }

   private fun conventions() = mapOf(
      OrbitalConfigKey to OrbitalConfigPathEntry
   )


}
