package io.vyne.spring.http.auth

import io.vyne.auth.tokens.AuthTokenProvider
import io.vyne.models.TypedInstance
import io.vyne.schemas.OperationNames
import io.vyne.schemas.RemoteOperation
import io.vyne.spring.http.HttpRequestFactory
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
