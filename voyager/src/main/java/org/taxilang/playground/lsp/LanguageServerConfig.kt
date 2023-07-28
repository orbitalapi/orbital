package org.taxilang.playground.lsp

import org.springframework.boot.context.properties.ConfigurationProperties

//@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.language-server")
data class LanguageServerConfig(
   val maxClients: Int = 100,
   val path: String = "/api/language-server",
)
