package io.vyne.auth

import mu.KotlinLogging
import org.http4k.core.cookie.Cookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.oauth2.server.resource.BearerTokenErrors
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern

private val logger = KotlinLogging.logger {}

/**
 * This is an implementation based on springs ServerBearerTokenAuthenticationConverter
 * however modified to accept the Authorization from either a Header, or a cookie.
 *
 * We allow this to support auth on Websockets and SSE requests, neither of which support
 * setting headers on outbound requests from the browser.
 *
 * Unfortunately, most of this is a copy-and-paste job, as the ServerBearerTokenAuthenticationConverter
 * class is largely private.
 */
class CookieOrHeaderTokenConverter : ServerAuthenticationConverter {

   companion object {
      private val authorizationPattern = Pattern.compile(
         "^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$",
         Pattern.CASE_INSENSITIVE
      )

      private fun invalidTokenError(): BearerTokenError? {
         return BearerTokenErrors.invalidToken("Bearer token is malformed")
      }
   }


   /**
    * Set if transport of access token using URI query parameter is supported. Defaults to `false`.
    *
    * The spec recommends against using this mechanism for sending bearer tokens, and even goes as far as
    * stating that it was only included for completeness.
    */
   var allowUriQueryParameter: Boolean = false

   override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
      return Mono.fromCallable { token(exchange.request) }
         .map { token: String? ->
            if (token.isNullOrEmpty()) {
               val error = invalidTokenError()
               throw OAuth2AuthenticationException(error)
            }
            BearerTokenAuthenticationToken(token)
         }
   }

   private fun token(request: ServerHttpRequest): String? {
      val authorizationHeaderToken = resolveFromAuthorizationHeader(request.headers)
      val authorizationCookieToken = resolveFromAuthorizationCookie(request.headers)
      val parameterToken = request.queryParams.getFirst("access_token")?.let { token ->
         if (isParameterTokenSupportedForRequest(request)) {
            token
         } else {
            null
         }
      }
      val tokens = listOfNotNull(authorizationHeaderToken, authorizationCookieToken, parameterToken)
      return when (tokens.size) {
         0 -> null
         1 -> tokens.first()
         else -> {
            if (authorizationHeaderToken != null) {
               authorizationHeaderToken
            } else {
               val error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request")
               throw OAuth2AuthenticationException(error)
            }
         }
      }
   }

   private fun resolveFromAuthorizationCookie(headers: HttpHeaders): String? {
      val cookies = (headers[HttpHeaders.COOKIE] ?: emptyList<String>())
         .mapNotNull { Cookie.parse(it) }
         .filter { it.name == HttpHeaders.AUTHORIZATION }
      return when {
         cookies.isEmpty() -> null
         cookies.size == 1 -> cookies.first().value
         else -> {
            val error =
               BearerTokenErrors.invalidRequest("Found multiple ${HttpHeaders.AUTHORIZATION} cookies present in the request")
            throw OAuth2AuthenticationException(error)
         }
      }
   }

   private fun resolveFromAuthorizationHeader(headers: HttpHeaders): String? {
      val authorization = headers.getFirst(HttpHeaders.AUTHORIZATION)
      if (StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
         val matcher = authorizationPattern.matcher(authorization)
         if (!matcher.matches()) {
            val error = invalidTokenError()
            throw OAuth2AuthenticationException(error)
         }
         return matcher.group("token")
      }
      return null
   }

   private fun invalidTokenError(): BearerTokenError {
      return BearerTokenErrors.invalidToken("Bearer token is malformed")
   }

   private fun isParameterTokenSupportedForRequest(request: ServerHttpRequest): Boolean {
      return this.allowUriQueryParameter && HttpMethod.GET == request.method
   }
}
