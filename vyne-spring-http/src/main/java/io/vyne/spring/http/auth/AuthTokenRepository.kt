package io.vyne.spring.http.auth

import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.ServiceName
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.util.concurrent.ConcurrentHashMap

enum class AuthTokenType {
   Header {
      override fun applyTo(token: AuthToken, httpRequest: HttpEntity<*>): HttpEntity<*> {
         val headers = HttpHeaders()
         headers.addAll(httpRequest.headers)
         val headerValue = if(!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
         headers.set(token.paramName, headerValue)
         return HttpEntity(
            httpRequest.body,
            headers
         )
      }
      override fun queryParams(token: AuthToken): MultiValueMap<String, String>? = null
   },
   QueryParam {
      override fun applyTo(token: AuthToken, httpRequest: HttpEntity<*>): HttpEntity<*> {
        return httpRequest
      }
      override fun queryParams(token: AuthToken): MultiValueMap<String, String>? {
         val queryParamValue = if(!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
         val queryParamMultiMap =  LinkedMultiValueMap<String, String>()
         queryParamMultiMap.add(token.paramName, queryParamValue)
         return queryParamMultiMap
      }

   },
   Cookie {
      override fun applyTo(token: AuthToken, httpRequest: HttpEntity<*>): HttpEntity<*> {
         val headers = HttpHeaders()
         headers.addAll(httpRequest.headers)
         val cookieValue = if(!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
         headers.set("Cookie", "${token.paramName}=$cookieValue")
         return HttpEntity(
            httpRequest.body,
            headers
         )
      }

      override fun queryParams(token: AuthToken): MultiValueMap<String, String>? = null
   };

   abstract fun applyTo(token: AuthToken, httpRequest: HttpEntity<*>): HttpEntity<*>
   abstract fun queryParams(token: AuthToken): MultiValueMap<String, String>?
}


data class AuthConfig(
   val authenticationTokens: MutableMap<ServiceName, AuthToken> = ConcurrentHashMap()
)

data class AuthToken(
   val tokenType: AuthTokenType,
   val value: String,
   val paramName: String,
   val valuePrefix: String? = null
) {
   fun applyTo(httpRequest: HttpEntity<*>): HttpEntity<*> {
      return tokenType.applyTo( this, httpRequest)
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

