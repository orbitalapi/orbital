package com.orbitalhq.cockpit.core.security.authentication.saml

import com.orbitalhq.cockpit.core.security.UserAuthenticatedEvent
import com.orbitalhq.cockpit.core.security.UserAuthenticatedEventSource
import com.orbitalhq.utils.RetryFailOnSerializeEmitHandler
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

class SamlUserAuthenticatedEventSource : UserAuthenticatedEventSource {
   private val userObserved = Sinks.many().unicast().onBackpressureBuffer<UserAuthenticatedEvent>()
   override val userAuthenticated: Flux<UserAuthenticatedEvent>
      get() = userObserved.asFlux()

   override fun onUserAuthenticated(events: List<UserAuthenticatedEvent>) {
      events.forEach { userObserved.emitNext(it, RetryFailOnSerializeEmitHandler) }
   }

}
