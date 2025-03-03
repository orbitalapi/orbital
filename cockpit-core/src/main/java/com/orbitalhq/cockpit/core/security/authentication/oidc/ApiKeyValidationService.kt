package com.orbitalhq.cockpit.core.security.authentication.oidc

import org.springframework.security.core.Authentication
import reactor.core.publisher.Mono

/**
 * Marker interface for services that exchange an API key for an authentication.
 */
interface ApiKeyValidator {
   fun authenticate(authentication: Authentication): Mono<Authentication>
}
