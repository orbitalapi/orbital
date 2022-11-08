package io.vyne.models.functions.stdlib.collections

import io.vyne.models.functions.NamedFunctionInvoker

object CollectionFiltering {
   val functions: List<NamedFunctionInvoker> = listOf(
      Single,
      SingleBy,
      FilterAll,
   )
}


