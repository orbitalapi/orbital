package io.vyne.jwt.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "keycloak.server")
data class KeycloakServerProperties(
   val contextPath: String,
   val realmImportFile: String,
   val adminUser: AdminUser,
   val realmUsers: List<RealmUser>)

@ConstructorBinding
data class AdminUser(val username: String, val password: String)

@ConstructorBinding
data class RealmUser(val username: String, val password: String)
