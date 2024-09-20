package com.orbitalhq.cockpit.core.security.authorisation.propelauth

import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import com.orbitalhq.cockpit.core.security.authorisation.propelauth.CloudPropelAuthClaimsExtractor.Companion.PropelAuthJwtKind
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Extracts org based roles from a Propel Auth token
 * when running self-hosted.
 *
 * Users may only belong to a single organisation.
 *
 * This is the default approach for self-hosted installs that have not switched to their
 * own IDP
 */
@Component
@Profile("!cloud")
@ConditionalOnProperty(
   name = ["vyne.security.open-idp.roles.format"],
   havingValue = PropelAuthJwtKind,
   matchIfMissing = false
)
class SelfHostedPropelAuthClaimsExtractor : JwtRolesExtractor {
   companion object {
      private val logger = KotlinLogging.logger {}
   }
   init {
       logger.info { "Using ${this::class.simpleName}" }
   }
   override fun getRoles(jwt: Jwt): Set<String> {
      val orgClaims = PropelAuthOrgExtractor.getSingleOrgClaims(jwt)
      return getOrgRoles(orgClaims)
   }
}

internal fun getOrgRoles(orgClaims: Map<String, Any>): Set<String> {
   val orgRoles = orgClaims["inherited_user_roles_plus_current_role"] as List<String>
   return orgRoles.toSet()
}
