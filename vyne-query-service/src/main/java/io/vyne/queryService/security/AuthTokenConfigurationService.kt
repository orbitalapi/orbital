package io.vyne.queryService.security

import io.vyne.queryService.BadRequestException
import io.vyne.spring.http.auth.AuthToken
import io.vyne.spring.http.auth.AuthTokenRepository
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
   fun submitToken(@PathVariable("serviceName") serviceName: String, @RequestBody token: AuthToken) {
      if (!repository.writeSupported) {
         throw BadRequestException("Modifying authentication tokens is not supported because no store has been configured.")
      }
      logger.info { "Updating token for service $serviceName to new token of type ${token.tokenType}" }
      repository.saveToken(serviceName, token)
      logger.info { "Token for service $serviceName has been updated" }
   }

}
