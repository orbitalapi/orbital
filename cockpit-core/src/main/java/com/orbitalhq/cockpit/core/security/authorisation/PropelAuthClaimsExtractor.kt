package com.orbitalhq.cockpit.core.security.authorisation

import com.orbitalhq.auth.authentication.PropelAuthJwtTokenClaims
import com.orbitalhq.cockpit.core.security.authorisation.PropelAuthClaimsExtractor.Companion.PropelAuthJwtKind
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["vyne.security.open-idp.jwt-type"], havingValue = PropelAuthJwtKind, matchIfMissing = false)
class PropelAuthClaimsExtractor : JwtRolesExtractor {
   companion object {
      const val PropelAuthJwtKind = "propelauth"
   }
   private val logger = KotlinLogging.logger {}
   override fun getRoles(jwt: Jwt):Set<String> {
      val orgInfoMap = jwt.claims[PropelAuthJwtTokenClaims.OrgIdToMemberInfo] as Map<String,Any>

      if (orgInfoMap.keys.isEmpty()) {
         logger.warn { "Provided PropelAuth JWT claims did not contain any organisations" }
         return emptySet()
      }

      // TODO : In future, we'll support multiple orgs
      val orgClaims = if (orgInfoMap.keys.size > 1) {
         val key = orgInfoMap.keys.first()
         logger.warn { "Provided PropelAuth JWT claims contains multiple organisations - which is not yet supported, just using the first - $key" }
         orgInfoMap[key] as Map<String,Any>
      } else {
         val key = orgInfoMap.keys.single()
         orgInfoMap[key] as Map<String,Any>
      }
      val orgRoles = orgClaims["inherited_user_roles_plus_current_role"] as List<String>
      return orgRoles.toSet()
   }
}
