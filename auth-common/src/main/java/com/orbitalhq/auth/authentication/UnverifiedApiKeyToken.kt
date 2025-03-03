package com.orbitalhq.auth.authentication

import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken

/**
 * An Api key that was passed by PropelAuth, and hos not yet been verified via the
 * auth token flow
 */
class UnverifiedApiKeyToken(token:String): BearerTokenAuthenticationToken(token) {
}
