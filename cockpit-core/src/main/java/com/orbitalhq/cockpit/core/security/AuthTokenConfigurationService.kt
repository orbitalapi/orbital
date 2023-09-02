package com.orbitalhq.cockpit.core.security

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.UriSafePackageIdentifier
import com.orbitalhq.auth.schemes.AuthScheme
import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.auth.schemes.SanitizedAuthScheme
import com.orbitalhq.auth.tokens.AuthToken
import com.orbitalhq.auth.tokens.AuthTokenRepository
import com.orbitalhq.auth.tokens.NoCredentialsAuthToken
import com.orbitalhq.schemas.ServiceName
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
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
   private val authSchemeRepository: AuthSchemeRepository
) {

   private val logger = KotlinLogging.logger {}

//   @PostMapping("/api/tokens/service/{serviceName}")
//   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
//   fun submitToken(
//      @PathVariable("serviceName") serviceName: String,
//      @RequestBody token: AuthToken
//   ): Mono<NoCredentialsAuthToken> {
//      if (!repository.writeSupported) {
//         throw BadRequestException("Modifying authentication tokens is not supported because no store has been configured.")
//      }
//      logger.info { "Updating token for service $serviceName to new token of type ${token.tokenType}" }
//      repository.saveToken(serviceName, token)
//      logger.info { "Token for service $serviceName has been updated" }
//      return Mono.just(NoCredentialsAuthToken(serviceName, token.tokenType))
//   }

   @PostMapping("/api/tokens/{packageIdentifier}/service/{serviceName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
   fun submitAuthScheme(
      @PathVariable("packageIdentifier") uriSafePackageIdentifier: UriSafePackageIdentifier,
      @PathVariable("serviceName") serviceName: String,
      @RequestBody token: AuthScheme
   ): Mono<SanitizedAuthScheme> {
      val packageIdentifier: com.orbitalhq.PackageIdentifier =
         com.orbitalhq.PackageIdentifier.fromUriSafeId(uriSafePackageIdentifier)
      val sanitizedAuthScheme = authSchemeRepository.saveToken(packageIdentifier, serviceName, token)
      return Mono.just(sanitizedAuthScheme)
   }


   @DeleteMapping("/api/tokens/{packageIdentifier}service/{serviceName}")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditAuthenticationTokens}')")
   fun deleteToken(
      @PathVariable("packageIdentifier") uriSafePackageIdentifier: UriSafePackageIdentifier,
      @PathVariable("serviceName") serviceName: String): Mono<Unit> {
      authSchemeRepository.deleteToken(PackageIdentifier.fromUriSafeId(uriSafePackageIdentifier), serviceName)
      return Mono.just(Unit)
   }

   @GetMapping("/api/tokens")
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewAuthenticationTokens}')")
   fun listTokens(): Mono<Map<ServiceName, AuthScheme>> {
      return Mono.just(this.authSchemeRepository.listTokensWithoutCredentials())
   }
}

