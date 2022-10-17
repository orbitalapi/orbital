package io.vyne.auth.authentication

import io.vyne.auth.authorisation.VyneUserName
import io.vyne.security.VyneGrantedAuthorities
import java.util.concurrent.ConcurrentHashMap

data class VyneUser(
   // The id / subject, as provided by the auth provider.
   // Does uniquely identify the user
   val userId: String,
   // The users preferred username.  Used for display, does not necessarily
   // guarantee that can be used to identify the user.
   val username: VyneUserName,
   val email: String,
   val profileUrl: String? = null,
   /**
    * Name as defined in the OID spec:
    * End-User's full name in displayable form including all name parts,
    * possibly including titles and suffixes, ordered according to the End-User's
    * locale and preferences.
    */
   val name: String? = null,
   val grantedAuthorities: Set<VyneGrantedAuthorities> = emptySet(),
   val isAuthenticated: Boolean = false
) {
   companion object {

      fun anonymousUser(grantedAuthorities: Set<VyneGrantedAuthorities>) =
         VyneUser(
            userId = "Anonymous",
            "Anonymous",
            email = "anonymous@vyne.co",
            isAuthenticated = false,
            grantedAuthorities = grantedAuthorities
         )
   }

}


data class VyneUserConfigConfig(
   // Map Key is UserId
   val vyneUserMap: MutableMap<String, VyneUser> = ConcurrentHashMap()
)

