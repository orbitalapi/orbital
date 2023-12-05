package com.orbitalhq.cockpit.core.security.authorisation

import org.springframework.security.oauth2.jwt.Jwt

interface JwtRolesExtractor {
   fun getRoles(jwt: Jwt):Set<String>
}
