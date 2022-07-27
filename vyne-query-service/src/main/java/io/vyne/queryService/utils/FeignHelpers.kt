package io.vyne.queryService.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import feign.FeignException
import io.vyne.spring.http.BadRequestException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import reactor.core.publisher.Mono

fun <T> handleFeignErrors(method: () -> Mono<T>): Mono<T> {
   try {
      return method.invoke()
         .onErrorMap { e ->
            fun mapError(e: Throwable): Nothing {
               when (e) {
                  // This class doesn't exist after upgrade of Spring.  Will wait to find what error types have replaced it.
//                  is HystrixRuntimeException -> mapError(e.cause!!)
                  is FeignException -> {
                     val errorPayload = jacksonObjectMapper().readValue<Map<String, Any>>(e.contentUTF8())
                     val errorMessage = errorPayload["message"] as String? ?: e.message ?: e.contentUTF8()
                     throw BadRequestException(errorMessage)
                  }
                  // This class doesn't exist after upgrade of Spring.  Will wait to find what error types have replaced it.
//                  is ClientException -> {
//                     val eurekaExceptionPrefix = "Load balancer does not have available server for client:"
//                     val errorMessage = e.message.orEmpty()
//                     if (errorMessage.startsWith(eurekaExceptionPrefix)) {
//                        throw ServiceNotAvailableException.forServiceName(errorMessage.removePrefix(eurekaExceptionPrefix).trim())
//                     } else {
//                        throw ServiceNotAvailableException(errorMessage);
//                     }
//                  }
                  else -> throw e
               }
            }
            mapError(e)
         }
   } catch (e: FeignException.BadRequest) {
      throw BadRequestException(e.message ?: e.contentUTF8())
   }
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServiceNotAvailableException(message:String):RuntimeException(message) {
   companion object {
      fun forServiceName(serviceName:String) = ServiceNotAvailableException(message = "Service $serviceName does not appear to be running" )
   }
}
