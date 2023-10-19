package com.orbitalhq.config

import com.typesafe.config.Config
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.VersionedSource

/**
 * Writes config
 */
interface ConfigSourceWriter : ConfigSourceLoader {
   fun saveConfig(targetPackage: PackageIdentifier, updated: Config)
   fun save(targetPackage: PackageIdentifier, source: VersionedSource)

   val packageIdentifiers: List<PackageIdentifier>
   val configFileName : String
}


fun List<ConfigSourceWriter>.getWriter(targetPackage: PackageIdentifier, filename: String): ConfigSourceWriter {
   val writers = this.filter { it.packageIdentifiers.contains(targetPackage) }
      .filter { it.configFileName == filename }
   return writers.firstOrNull()
      ?: error("Expected to find exactly 1 writer for package ${targetPackage.id}, but found ${writers.size}")
}

fun List<ConfigSourceWriter>.hasWriter(targetPackage: PackageIdentifier): Boolean {
   val writers = this.filter { it.packageIdentifiers.contains(targetPackage) }
   return writers.size == 1
}

fun List<ConfigSourceLoader>.writers() = this.filterIsInstance<ConfigSourceWriter>()
