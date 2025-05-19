package com.orbitalhq.models.functions.stdlib.collections

import com.orbitalhq.models.functions.NamedFunctionInvoker

object CollectionFiltering {
   val functions: List<NamedFunctionInvoker> = listOf(
      Single,
      SingleBy,
      Filter,
      FilterEach,
      First,
      Last,
      ExactlyOne,
      GetAtIndex,
      Intersection,
      AnyMatch,
      None,
      All
   )
}


