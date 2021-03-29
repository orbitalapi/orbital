package io.vyne.queryService.security

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.FactSets
import io.vyne.query.Fact
import io.vyne.queryService.schemas.VyneTypes
import io.vyne.schemas.fqn

data class VyneUser(
   // The id / subject, as provided by the auth provider.
   // Does uniquely identify the user
   val userId: String,
   // The users preferred username.  Used for display, does not necessarily
   // guarantee that can be used to identify the user.
   val username: String,
   val email: String,
   val profileUrl: String? = null,
   /**
    * Name as defined in the OID spec:
    * End-User's full name in displayable form including all name parts,
    * possibly including titles and suffixes, ordered according to the End-User's
    * locale and preferences.
    */
   val name: String? = null
) {
   companion object {
      val USERNAME = "${VyneTypes.NAMESPACE}.Username".fqn()
      val USERNAME_TYPEDEF = """namespace ${USERNAME.namespace} {
         |   type ${USERNAME.name} inherits String
         |}""".trimMargin()
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
