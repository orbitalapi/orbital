package com.orbitalhq.cockpit.core.security.authentication.oidc

import com.orbitalhq.auth.authentication.getPreferredUserDisplayName
import com.orbitalhq.auth.authorisation.*
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEvent
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEventSource
import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

/**
 * Converts given jwt token to set of granted authorities.
 *
 * Also, emits a UserAuthenticatedEvent on every interaction.
 */
class GrantedAuthoritiesExtractor(
   vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository,
   private val rolesExtractor: JwtRolesExtractor
) : Converter<Jwt, Collection<GrantedAuthority>>, UserAuthenticatedEventSource {
   private val vyneRoleDefinitions = vyneUserRoleDefinitionRepository.findAll()

   /**
    * A Sink that is published to whenever we encounter a JWT token.
    * Consumers may use the attached Flux<> for building
    * user-related actions, such as creating and updating user records.
    *
    * Since we don't own the concept of the user (it's left to the IDP), this
    * is the most appropriate hook to capture user details presented in the JWT.
    */
   private val userObserved = Sinks.many().unicast().onBackpressureBuffer<UserAuthenticatedEvent>()
   override val userAuthenticated: Flux<UserAuthenticatedEvent>
      get() = userObserved.asFlux()

   override fun onUserAuthenticated(events: List<UserAuthenticatedEvent>) {
      events.forEach { event ->
         userObserved.emitNext(
            event,
            RetryFailOnSerializeEmitHandler
         )
      }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   /**
    * Reads the roles from the JWT, and converts to authorities using the mapping
    */
   override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
      val preferredUserName = getPreferredUserDisplayName(jwt.claims)
      val roles = extractRoles(jwt)

      // map assigned roles to set of granted authorities.
      val userGrantedAuthorities = roles.flatMap {
         vyneRoleDefinitions[it]?.grantedAuthorities ?: emptySet()
      }.toSet()

      val authorities =
         userGrantedAuthorities.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.constantValue) }

      onUserAuthenticated(listOf(
         UserAuthenticatedEvent(preferredUserName, jwt.claims, authorities)
      ))
      return authorities
   }

   private fun extractRoles(jwt: Jwt): Set<String> {
      return rolesExtractor.getRoles(jwt)
   }
}
