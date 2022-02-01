package io.vyne.queryService.security

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.FactSets
import io.vyne.query.Fact
import io.vyne.queryService.schemas.VyneTypes
import io.vyne.queryService.security.authorisation.VyneUserName
import io.vyne.schemas.fqn
import io.vyne.security.VyneGrantedAuthorities
import io.vyne.spring.http.auth.AuthToken
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import reactor.core.publisher.Flux
import java.nio.file.Path
import java.nio.file.Paths
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
      val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
      val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()

      fun anonymousUser(grantedAuthorities: Set<VyneGrantedAuthorities>) =
      VyneUser(userId = "Anonymous", "Anonymous", email = "anonymous@vyne.co", isAuthenticated = false, grantedAuthorities = grantedAuthorities)
   }

   @get:JsonIgnore
   @delegate:JsonIgnore
   val facts: Set<Fact> by lazy {
      setOf(
         Fact(USERNAME.fullyQualifiedName, username, FactSets.CALLER)
      )
   }
}

fun VyneUser?.facts(): Set<Fact> {
   return this?.facts ?: emptySet()
}

interface VyneUserRepository {
   fun findAll(): Flux<VyneUser>
}

data class VyneUserConfigConfig(
   // Map Key is UserId
   val vyneUserMap: MutableMap<String, VyneUser> = ConcurrentHashMap()
)

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.users")
data class VyneUserConfig(
   val configFile: Path = Paths.get("users.conf")
)
