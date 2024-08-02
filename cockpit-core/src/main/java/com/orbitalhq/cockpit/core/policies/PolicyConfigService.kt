package com.orbitalhq.cockpit.core.policies

import com.orbitalhq.AuthClaimType
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.QualifiedName
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * This is a service to help the UI guide users
 * through the precursor steps in setting up policies (ie.,
 * defining auth, adding a JWTCLaims model, etc)
 */
@RestController
class PolicyConfigService(
   private val schemaProvider: SchemaProvider,
) {

   @GetMapping("/api/policies/readiness")
   fun getPolicyState(
      @AuthenticationPrincipal auth: Mono<Authentication>,
   ): Mono<PolicySetupReadiness> {
      return auth.switchIfEmpty { Mono.just(AnonymousAuthenticationToken("anonymous", "anonymous", listOf(SimpleGrantedAuthority("none")))) }
         .map { authToken ->
            val authTokenTypes = schemaProvider.schema.types
               .filter { it.inheritsFromTypeNames.contains(AuthClaimType.AuthClaims) }
               .map { it.name }
            val jwtClaims = when (authToken) {
               is AnonymousAuthenticationToken -> emptyMap()
               is JwtAuthenticationToken -> authToken.token.claims
               else -> emptyMap() // How do we handle SAML?
            }
            PolicySetupReadiness(
               authenticationConfigured = authToken !is AnonymousAuthenticationToken,
               claims = jwtClaims,
               authTokenTypes
            )
         }
   }
}

data class PolicySetupReadiness(
   val authenticationConfigured: Boolean,
   val claims: Any,
   val authTokenTypes: List<QualifiedName>
)
