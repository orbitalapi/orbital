package com.orbitalhq.query.runtime.core

import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.query.SearchFailedException
import com.orbitalhq.query.runtime.FailedSearchResponse
import com.orbitalhq.query.runtime.core.gateway.HttpErrorResponse
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

   @ExceptionHandler(OrbitalQueryException::class)
   fun handleOrbitalQueryException(ex: OrbitalQueryException): ResponseEntity<Any?> {
      val (statusCode, errorBody, responseHeaders) = HttpErrorResponse.getErrorCodeAndPayload(ex)
      return ResponseEntity.status(statusCode.value())
         .headers { httpHeaders ->
            responseHeaders.forEach { header ->
               httpHeaders.addAll(header.key, header.value)
            }
         }.body(errorBody)
   }
}
