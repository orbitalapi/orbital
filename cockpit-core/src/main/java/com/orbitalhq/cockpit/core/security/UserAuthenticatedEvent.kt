package com.orbitalhq.cockpit.core.security

import org.springframework.security.core.GrantedAuthority

/**
 * An event that is emitted whenever a user is authenticated.
 * Note that this event comes on every single authentication, i.e.,
 * each API interaction.
 *
 * Consumers should consume sparingly.
 */
data class UserAuthenticatedEvent(val preferredUserName: String, val claims: Map<String,Any>,
                                  val authorities: List<GrantedAuthority>)
