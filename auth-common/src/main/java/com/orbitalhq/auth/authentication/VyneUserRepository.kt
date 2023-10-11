package com.orbitalhq.auth.authentication

import reactor.core.publisher.Flux

// Potentially deprecated.
// This is an abstraction that allows persistence and loading via
// config file.  However, that has limitations, and I think we should
// consider deprecating it in favour of simple (but much more scalable)
// db persistence.
interface VyneUserRepository {
   fun findAll(): Flux<VyneUser>
}
