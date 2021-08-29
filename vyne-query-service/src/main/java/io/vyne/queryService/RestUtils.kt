package io.vyne.queryService

import io.vyne.ExceptionProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message:String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message:String): RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPathException(message: String) : RuntimeException(message)

@Component
class VyneQueryServiceExceptionProvider: ExceptionProvider {
   override fun notFoundException(message: String): RuntimeException = NotFoundException(message)
   override fun badRequestException(message: String): RuntimeException = BadRequestException(message)
   override fun invalidPathException(message: String): RuntimeException = InvalidPathException(message)
}
