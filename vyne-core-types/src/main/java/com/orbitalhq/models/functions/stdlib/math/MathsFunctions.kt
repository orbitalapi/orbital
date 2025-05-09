package com.orbitalhq.models.functions.stdlib.math

import com.orbitalhq.models.functions.NamedFunctionInvoker

object MathsFunctions {
   val functions: List<NamedFunctionInvoker> = listOf(
      Sum,
      Max,
      Min,
      Round
   )
}
