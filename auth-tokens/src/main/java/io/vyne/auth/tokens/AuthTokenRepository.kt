package io.vyne.auth.tokens

import io.vyne.auth.schemes.AuthScheme
import io.vyne.auth.schemes.HttpHeader
import io.vyne.auth.schemes.QueryParam
import io.vyne.schemas.ServiceName
import kotlinx.serialization.Serializable
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
         val headerValue =
            if (!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
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
         val queryParamValue =
            if (!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
         val queryParamMultiMap = LinkedMultiValueMap<String, String>()
         queryParamMultiMap.add(token.paramName, queryParamValue)
         return queryParamMultiMap
      }

   },
   Cookie {
      override fun applyTo(token: AuthToken, httpRequest: HttpEntity<*>): HttpEntity<*> {
         val headers = HttpHeaders()
         headers.addAll(httpRequest.headers)
         val cookieValue =
            if (!token.valuePrefix.isNullOrBlank()) "${token.valuePrefix} ${token.value}" else token.value
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


@Serializable
data class AuthConfig(
   val authenticationTokens: MutableMap<ServiceName, AuthToken> = ConcurrentHashMap()
) : AuthTokenProvider {
   override fun getToken(serviceName: ServiceName): AuthToken? {
      return authenticationTokens[serviceName]
   }

}

/**
 * Deprecated, to use AuthScheme instead which
 * allows richer config for advanced auth schemes like OAuth
 */
@Deprecated("Use AuthScheme instead")
@Serializable
data class AuthToken(
   val tokenType: AuthTokenType,
   val value: String,
   val paramName: String,
   val valuePrefix: String? = null
) {
   fun applyTo(httpRequest: HttpEntity<*>): HttpEntity<*> {
      return tokenType.applyTo(this, httpRequest)
   }

   fun upgradeToAuthScheme(): AuthScheme {
      return when (tokenType) {
         AuthTokenType.Header -> HttpHeader(value, prefix = valuePrefix ?: "Bearer", headerName = paramName)
         AuthTokenType.QueryParam -> QueryParam(this.paramName, this.value)
         else -> error("Upgrading tokenType $tokenType is not supported")
      }
   }
}

data class NoCredentialsAuthToken(
   val serviceName: String,
   val tokenType: AuthTokenType
)

/**
 * Provides read-only access for looking up Auth Token
 */
@Deprecated("Use AuthSchemeProvider")
interface AuthTokenProvider {
   fun getToken(serviceName: ServiceName): AuthToken?
}

/**
 * Repository that supports writing of AuthTokens.
 * Note - AuthTokens are being replaced with AuthScheme,
 * however a replacement of this interface isn't yet ready.
 */
@Deprecated("Use AuthSchemeRepository where possible (not fully implemented yet)")
interface AuthTokenRepository : AuthTokenProvider {
   fun saveToken(serviceName: String, token: AuthToken)

   fun listTokens(): List<NoCredentialsAuthToken>
   fun deleteToken(serviceName: String)

   fun getAllTokens(): AuthConfig

   val writeSupported: Boolean
}

