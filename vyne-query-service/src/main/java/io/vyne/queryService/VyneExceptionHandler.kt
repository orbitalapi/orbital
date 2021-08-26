package io.vyne.queryService

import io.vyne.query.SearchFailedException
import io.vyne.queryService.query.FailedSearchResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class VyneExceptionHandler  {

    @ExceptionHandler(SearchFailedException::class)
    fun handleAsyncRequestTimeoutException(ex: SearchFailedException): ResponseEntity<Any>? {
      val resp = FailedSearchResponse(ex.message?: "No message provided", null, queryId = "")
      return ResponseEntity(resp, HttpStatus.BAD_REQUEST)
   }
}

