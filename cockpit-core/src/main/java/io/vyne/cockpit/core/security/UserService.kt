package io.vyne.cockpit.core.security

import io.vyne.auth.authentication.VyneUser
import io.vyne.auth.authentication.VyneUserRepository
import io.vyne.auth.authentication.toVyneUser
import io.vyne.auth.authorisation.VyneUserRoleDefinitionRepository
import io.vyne.auth.authorisation.VyneUserRoleMappingRepository
import io.vyne.cockpit.core.security.authorisation.VyneOpenIdpConnectConfig
import io.vyne.security.VyneGrantedAuthorities
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestController
class UserService(
   private val vyneUserRepository: VyneUserJpaRepository,
   private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
   private val vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository,
   private val openIdpConfiguration: VyneOpenIdpConnectConfig
) {

   companion object {
      private const val MAX_COOKIE_SIZE = 4096

      /**
       * If the provided JWT does not have an expiration, use this default.
       */
      private val DEFAULT_COOKIE_DURATION = Duration.ofMinutes(60)
   }

   @GetMapping("/api/user")
   fun currentUserInfo(@AuthenticationPrincipal auth: Mono<Authentication>): Mono<ResponseEntity<VyneUser>> {
      return if (this.openIdpConfiguration.enabled) {
         auth
            .switchIfEmpty {
               error("Not Authorized")
            }
            .map { authentication ->
               if (authentication == null) {
                  ResponseEntity
                     .ok()
                     .body(VyneUser.anonymousUser(allGrantedAuthorities()))
               } else {
                  val vyneUserWithAuthorisation = withGrantedAuthorities(authentication.toVyneUser())
                  val response = ResponseEntity
                     .ok()
                  buildAuthCookie(authentication)?.let { cookie ->
                     response.header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                     )
                  }
                  response.body(vyneUserWithAuthorisation)
               }
            }
      } else {
         Mono.just(
            ResponseEntity
               .ok()
               .body(VyneUser.anonymousUser(allGrantedAuthorities()))
         )
      }
   }

   private fun withGrantedAuthorities(authenticatedVyneUser: VyneUser): VyneUser {
//      return authenticatedVyneUser
      val userRoles = vyneUserRoleMappingRepository.findByUserName(authenticatedVyneUser.username)?.roles ?: emptySet()
      val grantedAuthorities = userRoles
         .flatMap { role -> vyneUserRoleDefinitionRepository.findByRoleName(role)?.grantedAuthorities ?: emptySet() }
         .toSet()
      return authenticatedVyneUser.copy(grantedAuthorities = grantedAuthorities)
   }

   private fun allGrantedAuthorities(): Set<VyneGrantedAuthorities> {
      return vyneUserRoleDefinitionRepository
         .findAll().flatMap { role -> role.value.grantedAuthorities }
         .toSet()
   }


   @GetMapping("/api/users")
   fun vyneUsers() = vyneUserRepository.findAll()

   /**
    * When calling the /api/user endpoint, we also set a cookie with the JWT token.
    * This is so that SSE and Websocket requests have a way of presenting auth.
    * Our preference is to use auth headers, but those transports don't support it.
    */
   private fun buildAuthCookie(auth: Authentication): ResponseCookie? {
      if (auth !is JwtAuthenticationToken) {
         logger.warn { "Generation of user cookie auth is not supported for auth type ${auth::class.simpleName}" }
         null
      }
      val jwtToken = auth as JwtAuthenticationToken
      val authToken = auth.token.tokenValue

      val authTokenSizeInBytes = authToken.toByteArray().size
      if (authTokenSizeInBytes > MAX_COOKIE_SIZE) {
         logger.warn { "Cannot set auth token as cookie, because at ${authTokenSizeInBytes}B exceeds the max cookie size of ${MAX_COOKIE_SIZE}B" }
         return null
      }

      val expirationDuration =
         jwtToken.token.expiresAt?.let { tokenExpiration -> Duration.between(Instant.now(), tokenExpiration) }
            ?: DEFAULT_COOKIE_DURATION
      return try {
         ResponseCookie.from(HttpHeaders.AUTHORIZATION, authToken)
            .httpOnly(true)
            .maxAge(expirationDuration)
            .build()
      } catch (e: Exception) {
         logger.warn { "Failed to create an auth cookie - An exception was thrown: ${e.message}" }
         null
      }
   }
}


