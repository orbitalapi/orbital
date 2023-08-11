package io.vyne.cockpit.core.security

import reactor.core.publisher.Flux

/**
 * Indicates that a user has been authenticated.
 *
 * Events are emitted frequently, (generally on every API endpoint interaction) and consumers should
 * consume events sparingly.
 */
interface UserAuthenticatedEventSource {
   val userAuthenticated: Flux<UserAuthenticatedEvent>
}
