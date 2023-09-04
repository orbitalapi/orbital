package com.orbitalhq.connectors.config.jdbc

data class JdbcUrlAndCredentials(
   val url: String,
   val username: String?,
   val password: String?
)
