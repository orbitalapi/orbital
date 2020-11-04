package io.vyne.queryService

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class VyneExceptionHandler(@Value("\${spring.mvc.async.request-timeout}") val asyncRequestTimeout: Int) : ResponseEntityExceptionHandler() {
   override fun handleAsyncRequestTimeoutException(ex: AsyncRequestTimeoutException, headers: HttpHeaders, status: HttpStatus, webRequest: WebRequest): ResponseEntity<Any>? {
      val resp = FailedSearchResponse("Query Execution time exceeds maximum allowed execution time - $asyncRequestTimeout milliseconds", null)
      return ResponseEntity(resp, HttpStatus.OK)
   }
}

