package org.taxilang.playground.lsp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.language-server")
data class LanguageServerConfig(
   val maxClients: Int = 100,
   val path: String = "/api/language-server",
)
