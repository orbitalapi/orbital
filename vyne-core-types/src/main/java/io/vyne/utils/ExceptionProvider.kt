package io.vyne.utils

interface ExceptionProvider {
   fun notFoundException(message: String): RuntimeException
   fun badRequestException(message: String): RuntimeException
   fun invalidPathException(message: String): RuntimeException
}
