package com.orbitalhq.auth.authentication

import reactor.core.publisher.Flux

interface VyneUserRepository {
   fun findAll(): Flux<VyneUser>
}
