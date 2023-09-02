package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.functions.NamedFunctionInvoker

object CollectionFiltering {
   val functions: List<NamedFunctionInvoker> = listOf(
      Single,
      SingleBy,
      FilterAll,
      First,
      Last,
      GetAtIndex
   )
}


