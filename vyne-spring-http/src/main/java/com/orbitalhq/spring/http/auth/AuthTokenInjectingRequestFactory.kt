package com.orbitalhq.spring.http.auth

import com.orbitalhq.auth.tokens.AuthTokenProvider
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.spring.http.HttpRequestFactory
import org.springframework.http.HttpEntity
import org.springframework.util.MultiValueMap

@Deprecated("use AuthWebClientCustomizer instead, which has support for async token generation, like OAuth")
class AuthTokenInjectingRequestFactory(
   private val requestFactory: HttpRequestFactory,
   private val tokenProvider: AuthTokenProvider
) : HttpRequestFactory {
   override fun buildRequestBody(operation: RemoteOperation, parameters: List<TypedInstance>): HttpEntity<*> {
      val httpRequest = requestFactory.buildRequestBody(operation, parameters)
      val (service, _) = OperationNames.serviceAndOperation(operation.qualifiedName)
      val token = tokenProvider.getToken(service)
      return token?.applyTo(httpRequest) ?: httpRequest
   }

   override fun buildRequestQueryParams(operation: RemoteOperation): MultiValueMap<String, String>? {
      val (service, _) = OperationNames.serviceAndOperation(operation.qualifiedName)
      return tokenProvider.getToken(service)?.let { it.tokenType.queryParams(it) }
   }
}
