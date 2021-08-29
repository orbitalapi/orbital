package io.vyne.spring.http.auth

/**
 * A default token repository for use when no backing file
 * has been configured.
 */
object EmptyAuthTokenRepository : AuthTokenRepository {
   override val writeSupported: Boolean = false
   override fun getToken(serviceName: String): AuthToken? {
      return null
   }

   override fun listTokens(): List<NoCredentialsAuthToken> = emptyList()
   override fun deleteToken(serviceName: String) {
      TODO("Not yet implemented")
   }

   override fun saveToken(serviceName: String, token: AuthToken) {
      error("This repository does not support updates")
   }
}
