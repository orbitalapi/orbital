package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.stdlib.collections.CollectionFiltering

object StdLib {
   val functions: List<NamedFunctionInvoker> = listOf(
      Strings.functions,
      Functional.functions,
      Collections.functions,
      CollectionFiltering.functions,
      ObjectFunctions.functions
   ).flatten()
}

