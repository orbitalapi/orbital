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
