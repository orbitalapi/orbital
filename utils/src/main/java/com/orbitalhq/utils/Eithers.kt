package com.orbitalhq.utils

import arrow.core.Either

fun <A, B> Either<A, B>.get(): Any {
   return when (this) {
      is Either.Left -> this.value!!
      is Either.Right -> this.value!!
      else -> error("Can only be left or right")
   }
}

fun <A,B> Either<A,B>.recoverWith(other: Either<A,B>): Either<A,B> {
   return when (this) {
      is Either.Left -> other
      else -> this
   }
}

fun <A,B> Either<A,B>.recoverWith(other: () -> Either<A,B>): Either<A,B> {
   return when (this) {
      is Either.Left -> other()
      else -> this
   }
}
