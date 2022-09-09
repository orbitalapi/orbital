package io.vyne.utils

import arrow.core.Either

fun <A, B> Either<A, B>.get(): Any {
   return when (this) {
      is Either.Left -> this.value!!
      is Either.Right -> this.value!!
      else -> error("Can only be left or right")
   }
}
