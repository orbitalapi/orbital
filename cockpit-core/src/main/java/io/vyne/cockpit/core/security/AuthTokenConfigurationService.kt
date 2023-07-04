package io.vyne.cockpit.core.security

import io.vyne.UriSafePackageIdentifier
import io.vyne.auth.schemes.AuthScheme
import io.vyne.auth.schemes.AuthSchemeRepository
import io.vyne.auth.schemes.SanitizedAuthScheme
import io.vyne.auth.tokens.AuthToken
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.auth.tokens.NoCredentialsAuthToken
import io.vyne.security.VynePrivileges
import io.vyne.spring.http.BadRequestException
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Service which handles the saving / updating of auth tokens
 * for use in outbound calls that Vyne makes to other services.
 *
 * Has nothing to do with authentication into Vyne
 */
@RestController
class AuthTokenConfigurationService(
   private val repository: AuthTokenRepository,
   private val authSchemeRepository: AuthSchemeRepository
) {

   private val logger = KotlinLogging.logger {}

   @PostMapping("/api/tokens/service/{serviceName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
   fun submitToken(
      @PathVariable("serviceName") serviceName: String,
      @RequestBody token: AuthToken
   ): Mono<NoCredentialsAuthToken> {
      if (!repository.writeSupported) {
         throw BadRequestException("Modifying authentication tokens is not supported because no store has been configured.")
      }
      logger.info { "Updating token for service $serviceName to new token of type ${token.tokenType}" }
      repository.saveToken(serviceName, token)
      logger.info { "Token for service $serviceName has been updated" }
      return Mono.just(NoCredentialsAuthToken(serviceName, token.tokenType))
   }

   @PostMapping("/api/tokens/{packageIdentifier}/service/{serviceName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
   fun submitAuthScheme(
      @PathVariable("packageIdentifier") uriSafePackageIdentifier: UriSafePackageIdentifier,
      @PathVariable("serviceName") serviceName: String,
      @RequestBody token: AuthScheme
   ): Mono<SanitizedAuthScheme> {
      val packageIdentifier: io.vyne.PackageIdentifier =
         io.vyne.PackageIdentifier.fromUriSafeId(uriSafePackageIdentifier)
      val sanitizedAuthScheme = authSchemeRepository.saveToken(packageIdentifier, serviceName, token)
      return Mono.just(sanitizedAuthScheme)
   }


   @DeleteMapping("/api/tokens/service/{serviceName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
   fun deleteToken(@PathVariable("serviceName") serviceName: String): Mono<Unit> {
      this.repository.deleteToken(serviceName)
      return Mono.just(Unit)
   }

   @GetMapping("/api/tokens")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewAuthenticationTokens}')")
   fun listTokens(): Flux<NoCredentialsAuthToken> {
      return Flux.fromIterable(this.repository.listTokens())
   }
}

