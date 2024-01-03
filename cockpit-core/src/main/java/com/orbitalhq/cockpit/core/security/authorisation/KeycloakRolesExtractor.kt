package com.orbitalhq.cockpit.core.security.authorisation

import com.orbitalhq.cockpit.core.security.authorisation.KeycloakRolesExtractor.Companion.KeycloakJwtRolesFormat
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["vyne.security.open-idp.roles.format"], havingValue = KeycloakJwtRolesFormat, matchIfMissing = true)
class KeycloakRolesExtractor : JwtRolesExtractor, BasePathRolesExtractor(path = "realm_access.roles") {
   companion object {
      private val logger = KotlinLogging.logger {}
      const val KeycloakJwtRolesFormat = "keycloak"

      // Claims keys
      const val RealmAccess = "realm_access"
      const val Roles = "roles"
   }
}
