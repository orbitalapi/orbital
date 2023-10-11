package com.orbitalhq.spring.http

import com.orbitalhq.utils.ExceptionProvider
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message:String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message:String): RuntimeException(message)

fun badRequest(message: String):Nothing = throw BadRequestException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPathException(message: String) : RuntimeException(message)


class VyneQueryServiceExceptionProvider: ExceptionProvider {
   override fun notFoundException(message: String): RuntimeException = NotFoundException(message)
   override fun badRequestException(message: String): RuntimeException = BadRequestException(message)
   override fun invalidPathException(message: String): RuntimeException = InvalidPathException(message)
}

