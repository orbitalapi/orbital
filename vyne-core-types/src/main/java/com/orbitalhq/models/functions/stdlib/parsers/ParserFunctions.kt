package com.orbitalhq.models.functions.stdlib.parsers

import com.orbitalhq.models.functions.NamedFunctionInvoker

object ParserFunctions {
   val functions: List<NamedFunctionInvoker> = listOf(
      ParseJson
   )
}
