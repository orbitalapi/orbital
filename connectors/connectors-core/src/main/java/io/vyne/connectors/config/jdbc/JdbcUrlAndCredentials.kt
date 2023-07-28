package io.vyne.connectors.config.jdbc

data class JdbcUrlAndCredentials(
   val url: String,
   val username: String?,
   val password: String?
)
