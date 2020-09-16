package io.vyne.models.functions.stdlib

import io.vyne.VersionedSource
import io.vyne.models.functions.SelfDescribingFunction

object StdLib {
   val functions = listOf(
      Strings.functions
   ).flatten()
}

