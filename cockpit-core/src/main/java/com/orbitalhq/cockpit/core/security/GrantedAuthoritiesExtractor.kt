package com.orbitalhq.cockpit.core.security

import com.orbitalhq.auth.authentication.JwtStandardClaims
import com.orbitalhq.auth.authorisation.*
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
   private val vyneUserRoleMappingRepository: VyneUserRoleMappingRepository,
   vyneUserRoleDefinitionRepository: VyneUserRoleDefinitionRepository,
   private val adminRoleName: UserRole
) : Converter<Jwt, Collection<GrantedAuthority>>, UserAuthenticatedEventSource {
   private val vyneRoleDefinitions = vyneUserRoleDefinitionRepository.findAll()
   private val defaultClientUserRoles = vyneUserRoleDefinitionRepository.defaultUserRoles().roles
   private val defaultApiClientUserRoles = vyneUserRoleDefinitionRepository.defaultApiClientUserRoles().roles

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

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   /**
    * Populates set of granted authorities from 'sub' claim of the received jwt token.
    * 'sub' claim value gives us the actual username. From the 'username', first we find the corresponding roles
    * defined for the user and then for each role we fetch the corresponding granted authorities.
    *
    */
   override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
      val preferredUserName = jwt.claims[JwtStandardClaims.PreferredUserName] as String
      val client = jwt.claims[JwtStandardClaims.ClientId]


      if (client != null) {
         // The call came from an api client.
         assignDefaultRoleToApiClient(preferredUserName)
      } else {
         //First check whether this is the first user logged on to the system.
         checkFirstUserLogin(preferredUserName)
      }

      // find all the roles assigned to user.
      val userRoles = vyneUserRoleMappingRepository.findByUserName(preferredUserName)
      if (userRoles == null) {
         // We see the user first the very first time, assign the user to defaultUserRoles.
         logger.info { "A Vyne human user with Preferred User Name $preferredUserName logs in as the first user, assigning ${this.defaultClientUserRoles}" }
         vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = defaultClientUserRoles))
      }

      // map assigned roles to set of granted authorities.
      val userGrantedAuthorities = userRoles?.roles?.flatMap {
         vyneRoleDefinitions[it]?.grantedAuthorities ?: emptySet()
      }?.toSet() ?: emptySet()

      val authorities =
         userGrantedAuthorities.map { grantedAuthority -> SimpleGrantedAuthority(grantedAuthority.constantValue) }

      userObserved.emitNext(
         UserAuthenticatedEvent(preferredUserName, jwt.claims, authorities),
         RetryFailOnSerializeEmitHandler
      )
      return authorities
   }

   private fun checkFirstUserLogin(preferredUserName: String) {
      when (vyneUserRoleMappingRepository.size()) {
         0 -> {
            logger.info { "User With Preferred User Name $preferredUserName logs in as the first user, assigning $adminRoleName" }
            vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = setOf(adminRoleName)))
         }

         1 -> {
            // an api client could be the first one to invoke query server.
            val firstUser = vyneUserRoleMappingRepository.findAll().values.first()
            val firstUserName = vyneUserRoleMappingRepository.findAll().keys.first()
            if (firstUserName != preferredUserName && firstUser.type == VyneConsumerType.API.name) {
               // that means the first call to query server came from an api client.
               logger.info { "User With Preferred User Name $preferredUserName logs in as the first user, assigning $adminRoleName" }
               vyneUserRoleMappingRepository.save(preferredUserName, VyneUserRoles(roles = setOf(adminRoleName)))
            }
         }
      }
   }

   /**
    * Invoked only for api client calls, and relies on the fact that Token contains the 'ClientId' claim
    * (TODO this assumption, existence of clientId claim for Client_credentials based flow, is probably only valid for KeyCloack)
    */
   private fun assignDefaultRoleToApiClient(preferredUserName: String) {
      if (vyneUserRoleMappingRepository.findByUserName(preferredUserName) == null) {
         logger.info { "Api Client Preferred User Name $preferredUserName logs in as the first user, assigning ${this.defaultApiClientUserRoles}" }
         vyneUserRoleMappingRepository.save(
            preferredUserName,
            VyneUserRoles(roles = defaultApiClientUserRoles, type = VyneConsumerType.API.name)
         )
      }
   }
}
