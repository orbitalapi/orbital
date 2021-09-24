package io.vyne.pipelines.jet

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message) {

   companion object {
      private val logger = KotlinLogging.logger {}
      fun throwIf(condition: Boolean, message: String) {
         if (condition) {
            logger.warn { message }
            throw BadRequestException(message)
         }
      }
   }
}

fun badRequest(message:String):Nothing {
   throw BadRequestException(message)
}

