package io.vyne.security


data class VyneUser(
   /**
   The id / subject, as provided by the auth provider.
   Does uniquely identify the user
    */
   val userId: String,
   /**
   The users preferred username.  Used for display, does not necessarily
   guarantee that can be used to identify the user.
    */
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
      /**
       * Used for testing
       */
      fun forUserId(userId: String): VyneUser {
         return VyneUser(
            userId,
            username = userId,
            email = "$userId@vyne.co"
         )
      }
   }
}
