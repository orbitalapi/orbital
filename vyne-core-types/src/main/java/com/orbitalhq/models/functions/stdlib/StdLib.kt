package com.orbitalhq.models.functions.stdlib

import com.orbitalhq.models.functions.NamedFunctionInvoker
import com.orbitalhq.models.functions.stdlib.collections.CollectionFiltering
import com.orbitalhq.models.functions.stdlib.transform.Transformations

object StdLib {
   val functions: List<NamedFunctionInvoker> = listOf(
      Strings.functions,
      Functional.functions,
      Collections.functions,
      CollectionFiltering.functions,
      ObjectFunctions.functions,
      Transformations.functions
   ).flatten()
}

