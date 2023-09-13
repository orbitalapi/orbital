package com.orbitalhq.cockpit.core

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "vyne.db")
data class DatabaseConfig(
   val username: String? = null,
   val password: String? = null,
   val host: String? = null,
   // NOTE:
   // These default values don't work in
   // application.yml string interpolation.
   val port: String = "5432",
   val database: String = "orbital"
)
