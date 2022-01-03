package io.vyne.queryService

import io.vyne.utils.orElse
import mu.KotlinLogging
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

private val logger = KotlinLogging.logger {}

// Annoyingly, it looks like Spring isn't logging these errors anymore since moving to WebFlux.  So, adding an exception handler.
@ControllerAdvice
class GlobalExceptionHandler {
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ExceptionHandler(DecodingException::class)
   fun handleConflict(request:ServerHttpRequest, exception: DecodingException) {
      logger.info { "Request ${request.id} rejected - ${exception::class.simpleName}  :${exception.message}.  Cause:  ${exception.cause?.message?.orElse("Null")}" }
   }
}
