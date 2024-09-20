package com.orbitalhq.cockpit.core.security.authorisation.propelauth

import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import com.orbitalhq.cockpit.core.security.authorisation.propelauth.CloudPropelAuthClaimsExtractor.Companion.PropelAuthJwtKind
import com.orbitalhq.cockpit.core.security.authorisation.propelauth.SelfHostedPropelAuthClaimsExtractor.Companion
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

/**
 * Extracts org based roles from a Propel Auth token
 * when running on Orbital's cloud instance.
 *
 * Supports users belonging to multiple organisations (but expects this instance is
 * configured for a specific org)
 */
@Component
@Profile("cloud")
@ConditionalOnProperty(name = ["vyne.security.open-idp.roles.format"], havingValue = PropelAuthJwtKind, matchIfMissing = false)
class CloudPropelAuthClaimsExtractor(
   /**
    * When running in the cloud, we have an instance-per-org model.
    * Therefore, each deployment is associated with exactly one organisation.
    * This value indicates which subdomain the current instance is configured for
    */
   @Value("\${vyne.security.openIdp.permittedOrganisation}")
   private val permittedOrganisation: String
) : JwtRolesExtractor {
   companion object {
      const val PropelAuthJwtKind = "propelauth"
      private val logger = KotlinLogging.logger {}
   }
   init {
      logger.info { "Using ${this::class.simpleName}" }
   }

   override fun getRoles(jwt: Jwt):Set<String> {

      val thisOrgClaims = PropelAuthOrgExtractor.getOrgClaims(jwt, permittedOrganisation)
      return getOrgRoles(thisOrgClaims)
   }
}
