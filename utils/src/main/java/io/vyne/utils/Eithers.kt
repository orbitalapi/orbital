package io.vyne.utils

import arrow.core.Either
import com.diffplug.common.base.Either.Right

fun <A, B> Either<A, B>.get(): Any {
   return when (this) {
      is Either.Left -> this.a!!
      is Either.Right -> this.b!!
      else -> error("Can only be left or right")
   }
}
