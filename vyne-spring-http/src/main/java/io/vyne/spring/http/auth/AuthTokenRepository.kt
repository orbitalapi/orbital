package io.vyne.spring.http.auth

import io.vyne.schemas.ServiceName
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders

enum class AuthTokenType {
   AuthorizationBearerHeader;

   fun applyTo(httpRequest: HttpEntity<*>, value: String): HttpEntity<*> {
      val headers = HttpHeaders()
      headers.addAll(httpRequest.headers)
      headers.setBearerAuth(value)
      return HttpEntity(
         httpRequest.body,
         headers
      )
   }
}


data class AuthConfig(
   val authenticationTokens: MutableMap<ServiceName, AuthToken> = mutableMapOf()
)

data class AuthToken(
   val tokenType: AuthTokenType,
   val value: String
) {
   fun applyTo(httpRequest: HttpEntity<*>): HttpEntity<*> {
      return tokenType.applyTo(httpRequest, value)
   }
}

data class NoCredentialsAuthToken(
   val serviceName: String,
   val tokenType: AuthTokenType
)


interface AuthTokenRepository {
   fun getToken(serviceName: String): AuthToken?
   fun saveToken(serviceName: String, token: AuthToken)

   fun listTokens():List<NoCredentialsAuthToken>
   fun deleteToken(serviceName: String)

   val writeSupported:Boolean
}

