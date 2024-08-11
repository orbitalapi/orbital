package com.orbitalhq.cockpit.core.security.authorisation.propelauth

import com.orbitalhq.cockpit.core.security.authorisation.JwtRolesExtractor
import com.orbitalhq.cockpit.core.security.authorisation.propelauth.PropelAuthClaimsExtractor.Companion.PropelAuthJwtKind
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["vyne.security.open-idp.roles.format"], havingValue = PropelAuthJwtKind, matchIfMissing = false)
class PropelAuthClaimsExtractor(
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
   }
   override fun getRoles(jwt: Jwt):Set<String> {

      val thisOrgClaims = PropelAuthOrgExtractor.getOrgClaims(jwt, permittedOrganisation)

      val orgRoles = thisOrgClaims["inherited_user_roles_plus_current_role"] as List<String>
      return orgRoles.toSet()
   }
}
