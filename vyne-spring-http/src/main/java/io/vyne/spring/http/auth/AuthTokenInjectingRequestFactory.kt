package io.vyne.spring.http.auth

import io.vyne.models.TypedInstance
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.spring.http.HttpRequestFactory
import org.springframework.http.HttpEntity

class AuthTokenInjectingRequestFactory(
   private val requestFactory: HttpRequestFactory,
   private val tokenRepository: AuthTokenRepository
) : HttpRequestFactory {
   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      val httpRequest = requestFactory.buildRequestBody(operation, parameters)
      val (service, operation) = OperationNames.serviceAndOperation(operation.qualifiedName)
      val token = tokenRepository.getToken(service)

      return if (token == null) {
         httpRequest
      } else {
         addAuthHeaders(httpRequest, token)
      }
   }

   private fun addAuthHeaders(httpRequest: HttpEntity<*>, token: AuthToken): HttpEntity<*> {
      return token.applyTo(httpRequest)
   }

}
