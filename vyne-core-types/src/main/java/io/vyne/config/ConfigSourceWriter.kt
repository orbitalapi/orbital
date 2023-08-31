package io.vyne.config

import com.typesafe.config.Config
import io.vyne.PackageIdentifier
import io.vyne.VersionedSource


// This isn't really implemented.
// Pausing, as I think we just won't
// support ui-based writes for Auth tokens
// for a while
interface ConfigSourceWriter : ConfigSourceLoader {
   fun saveConfig(updated: Config)
   fun save(source: VersionedSource)

   val packageIdentifier: PackageIdentifier
}


fun List<ConfigSourceWriter>.getWriter(targetPackage: PackageIdentifier): ConfigSourceWriter {
   val writers = this.filter { it.packageIdentifier == targetPackage }
   return writers.singleOrNull()
      ?: error("Expected to find exactly 1 writer for package ${targetPackage.id}, but found ${writers.size}")
}

fun List<ConfigSourceWriter>.hasWriter(targetPackage: PackageIdentifier): Boolean {
   val writers = this.filter { it.packageIdentifier == targetPackage }
   return writers.size == 1
}

fun List<ConfigSourceLoader>.writers() = this.filterIsInstance<ConfigSourceWriter>()
