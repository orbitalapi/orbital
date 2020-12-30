package io.vyne.jwt.auth.config

import org.keycloak.services.util.JsonConfigProviderFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RegularJsonConfigProviderFactory: JsonConfigProviderFactory()

fun Any.log(): Logger {
   return LoggerFactory.getLogger(this::class.java)
}
