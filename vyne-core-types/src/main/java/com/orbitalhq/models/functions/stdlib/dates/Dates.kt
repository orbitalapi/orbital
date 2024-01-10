package com.orbitalhq.models.functions.stdlib.dates

import com.orbitalhq.models.functions.NamedFunctionInvoker

object Dates {
   val functions: List<NamedFunctionInvoker> = listOf(
      AddDays,
      AddMinutes,
      AddSeconds,
      ParseDate,
      Now
   )
}
