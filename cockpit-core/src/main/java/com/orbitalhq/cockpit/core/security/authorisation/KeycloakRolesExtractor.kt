package com.orbitalhq.cockpit.core.security.authorisation

import com.orbitalhq.cockpit.core.security.authorisation.KeycloakRolesExtractor.Companion.KeycloakJwtKind
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["vyne.security.open-idp.jwt-type"], havingValue = KeycloakJwtKind, matchIfMissing = true)
class KeycloakRolesExtractor : JwtRolesExtractor, PathRolesExtractor(path = "realm_access.roles") {
   companion object {
      private val logger = KotlinLogging.logger {}
      const val KeycloakJwtKind = "keycloak"

      // Claims keys
      const val RealmAccess = "realm_access"
      const val Roles = "roles"
   }
}
