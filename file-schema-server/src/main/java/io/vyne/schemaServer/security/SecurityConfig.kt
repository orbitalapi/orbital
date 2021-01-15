package io.vyne.schemaServer.security

import io.vyne.security.AuthenticationToVyneUserConverter
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

object SecureProfile {
   const val SECURE = "secure"
}
@Profile(SecureProfile.SECURE)
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Import(value = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class, AuthenticationToVyneUserConverter::class])
class SecurityConfig : WebSecurityConfigurerAdapter() {
   override fun configure(http: HttpSecurity) {
      http
         // Below sets up the Vyne as a resource server, so Vyne will check if there is an 'access' token on every request
         // and whether the token is valid or not. In order to verify that a token is genuine, it'll talk to openID connect server
         // (see spring.security.oauth2.resourceserver.jwt.jwk-set-uri)
         .oauth2ResourceServer()
         .jwt()
   }
}
