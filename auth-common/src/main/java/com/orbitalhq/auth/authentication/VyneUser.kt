package com.orbitalhq.auth.authentication

import com.orbitalhq.auth.authorisation.UserRole
import com.orbitalhq.security.VyneGrantedAuthority
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Transient
import java.util.concurrent.ConcurrentHashMap

// The userId of a user as defined by their issuer.
typealias UserIssuerId = String

// The id of a user that's unique throughout
// Orbital
typealias UserOrbitalId = String

// The users preferred username.  Used for display, does not necessarily
// guarantee that can be used to identify the user.
typealias UserDisplayName = String

/**
 * Name as defined in the OID spec:
 * End-User's full name in displayable form including all name parts,
 * possibly including titles and suffixes, ordered according to the End-User's
 * locale and preferences.
 */
typealias UserFullDisplayName = String


@Entity(name = "USERS")
data class VyneUser(
   // The id / subject, as provided by the auth provider.
   // Does uniquely identify the user when combined with the issuer
   @Column(name = "id")
   @Id
   val id: UserOrbitalId,
   val issuer: String,
   // The users preferred username.  Used for display, does not necessarily
   // guarantee that can be used to identify the user.
   val username: UserDisplayName,
   val email: String,
   @Column(name = "profile_url")
   val profileUrl: String? = null,
   val name: UserFullDisplayName? = null,


   @ElementCollection
   @CollectionTable(name = "USER_ROLES", joinColumns = [JoinColumn(name = "user_id")])
   @Column(name = "user_role")
   val roles:Set<UserRole> = emptySet(),

   // Authorities aren't persisted, as these are managed upstream in the IDP.
   // However, we use them for sending to the UI
   // Note that these authorities aren't used when evaluating
   // authorization (that comes from the user credentials - see GrantedAuthoritiesExtractor
   // however, they're populated from the same place, so should be in sync.
   @Transient
   val grantedAuthorities: Collection<VyneGrantedAuthority> = emptySet(),

   // Not persisted, assigned at runtime
   @Transient
   val isAuthenticated: Boolean = true
) {
   companion object {
      fun anonymousUser(grantedAuthorities: Set<VyneGrantedAuthority>) =
         VyneUser(
            id = "Anonymous",
            issuer = "Orbital",
            "Anonymous",
            email = "anonymous@orbitalhq.com",
            grantedAuthorities = grantedAuthorities,
            isAuthenticated = false
         )
   }

}


data class VyneUserConfigConfig(
   // Map Key is UserId
   val vyneUserMap: MutableMap<String, VyneUser> = ConcurrentHashMap()
)

