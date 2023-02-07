package io.vyne.query.runtime.core

import io.vyne.query.SearchFailedException
import io.vyne.query.runtime.FailedSearchResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class VyneExceptionHandler {

   @ExceptionHandler(SearchFailedException::class)
   fun handleAsyncRequestTimeoutException(ex: SearchFailedException): ResponseEntity<Any>? {
      val resp = FailedSearchResponse(ex.message ?: "No message provided", null, queryId = "")
      return ResponseEntity(resp, HttpStatus.BAD_REQUEST)
   }
}

