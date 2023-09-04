package com.orbitalhq.spring.http.auth.schemes

import com.orbitalhq.auth.schemes.*
import com.orbitalhq.schemas.ServiceName
import com.orbitalhq.spring.http.auth.oauthAuthorizedClientManager
import mu.KotlinLogging
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.*
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

@Component
class AuthWebClientCustomizer(
   private val oauthClientManager: AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager,
   private val repository: AuthSchemeProvider,

   ) {
   companion object {
      private val logger = KotlinLogging.logger {}
      fun empty(): AuthWebClientCustomizer {
         return forTokens(AuthTokens(emptyMap()))
      }

      fun forTokens(authTokens: AuthTokens): AuthWebClientCustomizer {
         val authSchemeProvider = SimpleAuthSchemeProvider(authTokens)
         return AuthWebClientCustomizer(
            oauthAuthorizedClientManager(authSchemeProvider),
            authSchemeProvider
         )
      }

      val SERVICE_NAME_ATTRIBUTE = "serviceName"
   }

   /**
    * A filter function that looks up an attribute from the request,
    * and then configures auth
    *
    * If using this, ensure the webClient is called with attribute(SERVICE_NAME_ATTRIBUTE, theNameOfTheService)
    */
   val authFromServiceNameAttribute: ExchangeFilterFunction = object : ExchangeFilterFunction {
      override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
         val serviceName = request.attribute(SERVICE_NAME_ATTRIBUTE)
            .getOrNull() as ServiceName? ?: error("Attribute $SERVICE_NAME_ATTRIBUTE should be set")

         val authScheme = repository.getAuthScheme(serviceName)
         if (authScheme != null) {
            logger.info { "Service $serviceName matched against auth scheme ${authScheme::class.simpleName}" }
         } else {
            logger.info { "Service $serviceName not configured to use auth" }
         }
         val filterFunction = getFilterFunction(serviceName, authScheme) ?: return next.exchange(request)
         return filterFunction.filter(request, next)
      }
   }

   private fun getFilterFunction(serviceName: ServiceName, authScheme: AuthScheme?): ExchangeFilterFunction? {
      return when (authScheme) {
         null -> null
         is BasicAuth -> ExchangeFilterFunctions.basicAuthentication(
            authScheme.username,
            authScheme.password
         )

         is Cookie -> cookieFilterFunction(authScheme)
         is OAuth2 -> oauthFilterFunction(serviceName)
         is QueryParam -> queryParamFilterFunction(authScheme)
         is HttpHeader -> httpHeaderFilterFunction(authScheme)
         else -> error("Support for auth schema ${authScheme::class.simpleName} is not implemented")
      }
   }

   private fun cookieFilterFunction(authScheme: Cookie): ExchangeFilterFunction {
      return ExchangeFilterFunction.ofRequestProcessor { request ->
         Mono.just(
            ClientRequest.from(request)
               .cookie(authScheme.cookieName, authScheme.value)
               .build()
         )

      }
   }

   private fun httpHeaderFilterFunction(authScheme: HttpHeader): ExchangeFilterFunction {
      return ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
         val newRequest = ClientRequest.from(clientRequest)
            .header(authScheme.headerName, authScheme.prefixedValue())
            .build()

         Mono.just(newRequest)

      }
   }

   private fun queryParamFilterFunction(authScheme: QueryParam): ExchangeFilterFunction {
      return ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
         // Create a new URI with the query parameter appended
         val newUri = UriComponentsBuilder.fromUri(clientRequest.url())
            .queryParam(authScheme.parameterName, authScheme.value)
            .build()
            .toUri()

         // Create a new ClientRequest with the new URI
         val newRequest = ClientRequest.from(clientRequest)
            .url(newUri)
            .build()

         Mono.just(newRequest)
      }
   }

   private fun oauthFilterFunction(serviceName: ServiceName): ExchangeFilterFunction {
      val oauth2FilterFunction = ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthClientManager)
      oauth2FilterFunction.setDefaultOAuth2AuthorizedClient(true)
      oauth2FilterFunction.setDefaultClientRegistrationId(serviceName)
      return oauth2FilterFunction
   }
}

/**
 * Adds an attribute that is used by an AuthWebClientCustomizer
 * to look up the required AuthScheme.
 *
 * Also requires that the WebClient.Builder has been
 * prepared by adding a AuthWebClientCustomizer.authFromServiceNameAttribute filterFunction
 */
fun <S : RequestHeadersSpec<S>> RequestHeadersSpec<S>.addAuthTokenAttributes(serviceName: ServiceName): S {
   return this.attribute(AuthWebClientCustomizer.SERVICE_NAME_ATTRIBUTE, serviceName)
}
