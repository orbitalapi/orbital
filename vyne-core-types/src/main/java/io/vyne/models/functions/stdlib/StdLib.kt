package io.vyne.models.functions.stdlib

import io.vyne.models.functions.NamedFunctionInvoker
import io.vyne.models.functions.stdlib.collections.CollectionFiltering

object StdLib {
   val functions: List<NamedFunctionInvoker> = listOf(
      Strings.functions,
      Functional.functions,
      Collections.functions,
      CollectionFiltering.functions,
      ObjectFunctions.functions
   ).flatten()
}

