package io.vyne.queryService.security

import io.vyne.security.VynePrivileges
import io.vyne.spring.http.BadRequestException
import io.vyne.spring.http.auth.AuthToken
import io.vyne.spring.http.auth.AuthTokenRepository
import io.vyne.spring.http.auth.NoCredentialsAuthToken
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Service which handles the saving / updating of auth tokens
 * for use in outbound calls that Vyne makes to other services.
 *
 * Has nothing to do with authentication into Vyne
 */
@RestController
class AuthTokenConfigurationService(private val repository: AuthTokenRepository) {

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

