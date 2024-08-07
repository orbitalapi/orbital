package com.orbitalhq.auth.authentication

import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Provides an API for fetching a principal for when Orbital
 * executes background work (such as streaming queries)
 */
interface ExecutionPrincipalAuthenticationService {

   fun loadPrincipal():Mono<Principal>

   fun loadUser():Mono<VyneUser>
}
