package com.orbitalhq.cockpit.core.security

import com.orbitalhq.auth.CookieOrHeaderTokenConverter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.web.reactive.socket.HandshakeInfo
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

/**
 * Websocket handler which reads auth credentials from the originating
 * HTTP request, and adds authentication onto the session
 */
class AuthenticationWebSocketHandler(
   private val delegate: WebSocketHandler,
   private val authenticationManager: ReactiveAuthenticationManager,
   private val tokenConverter: CookieOrHeaderTokenConverter = CookieOrHeaderTokenConverter()
) : WebSocketHandler {

   override fun handle(session: WebSocketSession): Mono<Void> {
      val token = extractTokenFromSession(session)
         ?: return Mono.error(AuthenticationCredentialsNotFoundException("No valid token found in Websocket handshake"))

      return authenticate(token)
         .map { auth -> AuthenticatedWebsocketSession(session, auth) }
         .flatMap { delegate.handle(it) }
   }

   private fun extractTokenFromSession(session: WebSocketSession): String? {
      return tokenConverter.getTokenFromHeaders(session.handshakeInfo.headers)
   }

   private fun authenticate(token: String): Mono<Authentication> {
      val authenticationToken = BearerTokenAuthenticationToken(token)
      val auth = authenticationManager.authenticate(authenticationToken)
      return auth
   }
}

/**
 * Wrapper around a normal WebSocketSession but exposes the Authentication provided
 */
class AuthenticatedWebsocketSession(private val delegate: WebSocketSession,val authentication: Authentication) : WebSocketSession by delegate {
   override fun getHandshakeInfo(): HandshakeInfo {
      return HandshakeInfo(
         delegate.handshakeInfo.uri,
         delegate.handshakeInfo.headers,
         Mono.just(authentication),
         delegate.handshakeInfo.subProtocol
      )
   }
}
