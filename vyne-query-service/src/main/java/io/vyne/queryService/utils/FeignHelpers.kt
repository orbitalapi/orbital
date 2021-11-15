package io.vyne.queryService.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.netflix.client.ClientException
import com.netflix.hystrix.exception.HystrixRuntimeException
import feign.FeignException
import io.vyne.spring.http.BadRequestException
import reactor.core.publisher.Mono

fun <T> handleFeignErrors(method: () -> Mono<T>): Mono<T> {
   try {
      return method.invoke()
         .onErrorMap { e ->
            fun mapError(e: Throwable): Nothing {
               when (e) {
                  is HystrixRuntimeException -> mapError(e.cause!!)
                  is FeignException -> {
                     val errorPayload = jacksonObjectMapper().readValue<Map<String,Any>>(e.contentUTF8())
                     val errorMessage = errorPayload["message"] as String? ?: e.message ?: e.contentUTF8()
                     throw BadRequestException(errorMessage)
                  }
                  is ClientException -> {
                     throw BadRequestException(e.message!!)
                  }
                  else -> throw e
               }
            }
            mapError(e)
         }
   } catch (e: FeignException.BadRequest) {
      throw BadRequestException(e.message ?: e.contentUTF8())
   }
}
