package com.orbitalhq.spring.http.auth

import com.orbitalhq.auth.tokens.AuthConfig
import com.orbitalhq.auth.tokens.AuthToken
import com.orbitalhq.auth.tokens.AuthTokenRepository
import com.orbitalhq.auth.tokens.NoCredentialsAuthToken

/**
 * A default token repository for use when no backing file
 * has been configured.
 */
@Deprecated("Use EmptyAuthSchemeRepository instead")
object EmptyAuthTokenRepository : AuthTokenRepository {
   override val writeSupported: Boolean = false
   override fun getToken(serviceName: String): AuthToken? {
      return null
   }

   override fun getAllTokens(): AuthConfig = AuthConfig()

   override fun listTokens(): List<NoCredentialsAuthToken> = emptyList()
   override fun deleteToken(serviceName: String) {
      TODO("Not yet implemented")
   }

   override fun saveToken(serviceName: String, token: AuthToken) {
      error("This repository does not support updates")
   }
}
