package io.vyne.spring.http.auth

import io.vyne.models.TypedInstance
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.spring.http.HttpRequestFactory
import org.springframework.http.HttpEntity
import org.springframework.util.MultiValueMap

class AuthTokenInjectingRequestFactory(
   private val requestFactory: HttpRequestFactory,
   private val tokenRepository: AuthTokenRepository
) : HttpRequestFactory {
   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      val httpRequest = requestFactory.buildRequestBody(operation, parameters)
      val (service, _) = OperationNames.serviceAndOperation(operation.qualifiedName)
      val token = tokenRepository.getToken(service)
      return token?.applyTo(httpRequest) ?: httpRequest
   }

   override fun buildRequestQueryParams(operation: RemoteOperation): MultiValueMap<String, String>? {
      val (service, _) = OperationNames.serviceAndOperation(operation.qualifiedName)
      return tokenRepository.getToken(service)?.let { it.tokenType.queryParams(it) }
   }
}
