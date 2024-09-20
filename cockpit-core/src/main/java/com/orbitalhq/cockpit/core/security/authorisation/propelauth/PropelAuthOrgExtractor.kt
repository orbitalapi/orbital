package com.orbitalhq.cockpit.core.security.authorisation.propelauth

import com.orbitalhq.auth.authentication.PropelAuthJwtTokenClaims
import com.orbitalhq.cockpit.core.NotAuthorizedException
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Gets organisation data from a PropelAuth JWT
 * for the requested subdomain / organisation
 */
object PropelAuthOrgExtractor {
   fun getOrgClaims(jwt: Jwt, organisation: String):Map<String,Any> {
      val orgInfoMap = jwt.claims[PropelAuthJwtTokenClaims.OrgIdToMemberInfo] as Map<String,Map<String,Any>>

      if (orgInfoMap.keys.isEmpty()) {
         throw NotAuthorizedException("You are not a member of any organisations")
      }

      val organisationClaims = orgInfoMap.filter { (orgId,orgClaims) ->
         orgClaims["url_safe_org_name"] == organisation
      }
      if (organisationClaims.isEmpty()) {
         throw NotAuthorizedException("You are not a member of organisation $organisation")
      }
      val thisOrgClaims = organisationClaims.values.single()
      return thisOrgClaims
   }

   fun getSingleOrgClaims(jwt: Jwt): Map<String,Any> {
      val orgInfoMap = jwt.claims[PropelAuthJwtTokenClaims.OrgIdToMemberInfo] as Map<String,Map<String,Any>>

      if (orgInfoMap.keys.isEmpty()) {
         throw NotAuthorizedException("You are not a member of any organisations")
      }
      if (orgInfoMap.size != 1) {
         throw NotAuthorizedException("You are a member of multiple organisations - which is not supported when running self-hosted")
      }
      return orgInfoMap.values.single()
   }
}
