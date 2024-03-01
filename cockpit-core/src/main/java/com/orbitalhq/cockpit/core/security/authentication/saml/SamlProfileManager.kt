package com.orbitalhq.cockpit.core.security.authentication.saml

import com.orbitalhq.auth.authentication.AuthenticationType
import com.orbitalhq.auth.authentication.JwtStandardClaims
import com.orbitalhq.auth.authentication.OrbitalClaims
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEvent
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEventSource
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.UserProfile
import org.pac4j.saml.profile.SAML2Profile
import org.pac4j.springframework.security.profile.SpringSecurityProfileManager
import org.springframework.security.core.authority.SimpleGrantedAuthority


class SamlProfileManager(context: WebContext,
                         sessionStore: SessionStore,
                         private val userAuthenticatedEventSource: UserAuthenticatedEventSource
) :
   SpringSecurityProfileManager(context, sessionStore) {
   override fun saveAll(profiles: LinkedHashMap<String, UserProfile>, saveInSession: Boolean) {
      super.saveAll(profiles, saveInSession)
      logger.debug("Saml user Authentication => {}", profiles)
      val events = profiles.map { profile ->
         val saml2Profile = profile.value as SAML2Profile
         val preferredUserName = when {
            saml2Profile.displayName != null -> saml2Profile.displayName
            saml2Profile.firstName != null && saml2Profile.familyName != null -> "${saml2Profile.firstName} ${saml2Profile.familyName}"
            saml2Profile.email != null -> saml2Profile.email
            else -> saml2Profile.id
         }

         val authorities =
            saml2Profile.roles.map { role -> SimpleGrantedAuthority(role) }

         UserAuthenticatedEvent(
            preferredUserName,
            toClaimMap(saml2Profile, preferredUserName),
            authorities)
      }

      userAuthenticatedEventSource.onUserAuthenticated(events)
   }

   private fun toClaimMap(saml2Profile: SAML2Profile, preferredUserName: String): Map<String, Any> {
      return mapOf<String,Any>(
            JwtStandardClaims.Sub to  saml2Profile.id,
            JwtStandardClaims.Issuer to saml2Profile.issuerEntityID,
            JwtStandardClaims.Email to  (saml2Profile.email ?: saml2Profile.id),
            JwtStandardClaims.PreferredUserName to preferredUserName,
            JwtStandardClaims.PictureUrl to  JwtStandardClaims.PictureUrl,
            JwtStandardClaims.Name to saml2Profile.displayName,
            OrbitalClaims.AuthType to AuthenticationType.Saml
       )
   }
}
