package io.vyne.config

import com.typesafe.config.Config
import io.vyne.PackageIdentifier


// This isn't really implemented.
// Pausing, as I think we just won't
// support ui-based writes for Auth tokens
// for a while
interface HoconWriter : HoconLoader {
   fun saveConfig(updated: Config)

   val packageIdentifier: PackageIdentifier


}
