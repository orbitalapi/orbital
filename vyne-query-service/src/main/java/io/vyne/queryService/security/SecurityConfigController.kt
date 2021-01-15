package io.vyne.queryService.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SecurityConfigController(
   @Value("\${vyne.security.issuerUrl:}") private val issuerUrl: String,
   @Value("\${vyne.security.clientId:}") private val clientId: String,
   @Value("\${vyne.security.scope:}") private val scope: String) {
   @GetMapping("/api/security/config")
   fun securityIssuerUrl() = FrontendConfig(issuerUrl, clientId, scope)


}

data class FrontendConfig(val issuerUrl: String, val clientId: String, val scope: String, val enabled: Boolean = issuerUrl.isNotBlank())


