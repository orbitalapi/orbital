package io.vyne.models.functions.stdlib

import io.vyne.models.functions.NamedFunctionInvoker

object StdLib {
   val functions: List<NamedFunctionInvoker> = listOf(
      Strings.functions,
      Functional.functions,
      Collections.functions,
      CollectionFiltering.functions
   ).flatten()
}

