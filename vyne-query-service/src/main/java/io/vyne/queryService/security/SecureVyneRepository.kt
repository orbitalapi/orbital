package io.vyne.queryService.security

import reactor.core.publisher.Flux

/**
 * Placeholder VyneUser Repositary when Vyne query server is in secure mode (i.e. when secure profile is activated amd
 * Vyne is in the role of oauth2ResourceServer)
 */
class SecureVyneRepository: VyneUserRepository {
   override fun findAll(): Flux<VyneUser> {
      return Flux.empty()
   }
}
