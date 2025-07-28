package com.orbitalhq.spring.http.auth.schemes

import com.orbitalhq.auth.schemes.AuthScheme
import com.orbitalhq.auth.schemes.AuthSchemeProvider
import com.orbitalhq.auth.schemes.AuthTokens
import com.orbitalhq.auth.schemes.BasicAuth
import com.orbitalhq.auth.schemes.Cookie
import com.orbitalhq.auth.schemes.HttpHeader
import com.orbitalhq.auth.schemes.MutualTls
import com.orbitalhq.auth.schemes.OAuth2
import com.orbitalhq.auth.schemes.QueryParam
import com.orbitalhq.auth.schemes.SimpleAuthSchemeProvider
import com.orbitalhq.schemas.ServiceName
import com.orbitalhq.spring.http.auth.OAuthRefreshTokenManager
import com.orbitalhq.spring.http.auth.oauthAuthorizedClientManager
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContext
import mu.KotlinLogging
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.netty.util.NettySslUtils
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.nio.file.Paths
import java.time.Duration
import kotlin.jvm.optionals.getOrNull


@Component
class AuthWebClientCustomizer(
   private val oauthClientManager: AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager,
   private val oauthClientService: ReactiveOAuth2AuthorizedClientService,
   private val repository: AuthSchemeProvider,
   ) {
   companion object {
      private val logger = KotlinLogging.logger {}
      fun empty(): AuthWebClientCustomizer {
         return forTokens(AuthTokens(emptyMap()))
      }

      /**
       * Creates a AuthWebClientCustomizer.
       * Can optionally also prepare OAUth refresh tokens, which is useful for
       * short-lived customizers (ie., for a single request).
       *
       * For flows where the AuthWebClientCustomizer lives across multiple requests,
       * you should register a dedicated OAuthRefreshTokenManager, which
       * listens for configuration changes.
       */
      fun forTokens(authTokens: AuthTokens, oneTimeRefreshTokenReset: Boolean = false): AuthWebClientCustomizer {
         val authSchemeProvider = SimpleAuthSchemeProvider(authTokens)
         val (authorizedClientService,oauthClientManager) = oauthAuthorizedClientManager(authSchemeProvider)
         if (oneTimeRefreshTokenReset) {
            OAuthRefreshTokenManager(authorizedClientService, authSchemeProvider).resetRefreshTokens()
         }

         return AuthWebClientCustomizer(
            oauthClientManager,
            authorizedClientService,
            authSchemeProvider
         )
      }

      const val SERVICE_NAME_ATTRIBUTE = "serviceName"
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
   fun reactorClientHttpConnector(mutualTls: MutualTls) =  ReactorClientHttpConnector(httpClient(mtlsSslContext(mutualTls)))

   fun mutualMtlsAuthScheme(serviceName: ServiceName): MutualTls? =  repository.getAuthScheme(serviceName) as? MutualTls

   private fun getFilterFunction(serviceName: ServiceName, authScheme: AuthScheme?): ExchangeFilterFunction? {
      return when (authScheme) {
         null -> null
         is BasicAuth -> ExchangeFilterFunctions.basicAuthentication(
            authScheme.username,
            authScheme.password
         )

         is Cookie -> cookieFilterFunction(authScheme)
         is OAuth2 -> oauthFilterFunction(serviceName, authScheme)
         is QueryParam -> queryParamFilterFunction(authScheme)
         is HttpHeader -> httpHeaderFilterFunction(authScheme)
         is MutualTls -> null
         else -> error("Support for auth schema ${authScheme::class.simpleName} is not implemented")
      }
   }

    fun httpClient(sslContext: SslContext?): HttpClient {
      val httpClient =  HttpClient.create(
         ConnectionProvider.builder("RestTemplateInvoker-Connection-Pool")
            .maxConnections(500)
            .maxIdleTime(Duration.ofMillis(10000.toLong()))
            .maxLifeTime(Duration.ofMinutes(1.toLong()))
            .metrics(true)
            .fifo()
            .pendingAcquireTimeout(Duration.ofMillis(20.toLong()))
            .build()
      )
         // 30 seconds as the default timeout, which is the spring boot default.
         // See ORB-997
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
         .keepAlive(true)
         .compress(true)

      return sslContext?.let { context ->
         httpClient.secure { sslSpec -> sslSpec.sslContext(context) }
      } ?: httpClient
   }

   private fun mtlsSslContext(authScheme: MutualTls): SslContext {
      val sslFactory = SSLFactory.builder()
         .withIdentityMaterial(Paths.get(authScheme.keystorePath), authScheme.keystorePassword.toCharArray())
         .withTrustMaterial(Paths.get(authScheme.truststorePath), authScheme.truststorePassword.toCharArray())
         .build()

      return NettySslUtils.forClient(sslFactory).build()
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

   private fun oauthFilterFunction(serviceName: ServiceName, authScheme: OAuth2): ExchangeFilterFunction {

      // See comments on RefreshTokenExchangeFilterFunction as to why we have to use a different
      // function here.
       return if (authScheme.grantType == OAuth2.AuthorizationGrantType.RefreshToken) {
         RefreshTokenExchangeFilterFunction(serviceName, authScheme, oauthClientService)
      } else {
         val oauth2FilterFunction = ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthClientManager)
         oauth2FilterFunction.setDefaultOAuth2AuthorizedClient(true)
         oauth2FilterFunction.setDefaultClientRegistrationId(serviceName)
         oauth2FilterFunction

      }
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
