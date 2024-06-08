package com.orbitalhq.config

import lang.taxi.packages.SourcesType

/**
 * Provides additionalSources section settings in taxi.conf files.
 */
object ConfigFileLocationConventions {
    var OrbitalConfigKey = "@orbital/config"
    var OrbitalConfigPathEntry = "orbital/config/*.conf"
    fun getConventionalPathEntry(sourcesType: SourcesType):String {
        return conventions()[sourcesType] ?: error("No convention exists for additional sources type of $sourcesType")
    }
    private fun conventions() = mapOf(
        OrbitalConfigKey to OrbitalConfigPathEntry
    )
}