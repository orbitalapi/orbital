package io.vyne.queryService.security

import io.vyne.security.VynePrivileges
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import reactor.core.publisher.Mono

/**
 * Just a workaround to perform authorisation check through spring reactive method security
 * for rest methods returning Flow.
 * These won't be required once we are on Spring Security 5.5 or higher.
 *
 */
interface QueryServerAuthorizationGuard {
   fun verifyQueryRunAuthority(): Mono<Void>
}

private val logger = KotlinLogging.logger {  }
open class SecureQueryServerAuthorizationGuard: QueryServerAuthorizationGuard {
   @PreAuthorize("hasAuthority('${VynePrivileges.RunQuery}')")
   override fun verifyQueryRunAuthority(): Mono<Void> {
      return Mono.empty()
   }
}

object UnSecureQueryServerAuthorizationGuard: QueryServerAuthorizationGuard {
   override fun verifyQueryRunAuthority(): Mono<Void>  { return Mono.empty() }
}
