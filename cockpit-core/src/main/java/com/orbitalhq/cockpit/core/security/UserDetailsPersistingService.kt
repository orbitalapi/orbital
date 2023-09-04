package com.orbitalhq.cockpit.core.security

import com.google.common.base.Ticker
import com.google.common.cache.CacheBuilder
import io.vyne.cockpit.core.security.VyneUserJpaRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import java.time.Duration

@Component
class UserDetailsPersistingService(
   eventSource: UserAuthenticatedEventSource,
   userRepository: VyneUserJpaRepository,
   persistFrequency: Duration = Duration.ofMinutes(15),
   ticker: Ticker = Ticker.systemTicker()
) {

   private val eventsToPersist = Sinks.many().unicast().onBackpressureBuffer<UserAuthenticatedEvent>()
   private val cachedEvents = CacheBuilder
      .newBuilder()
      .ticker(ticker)
      .expireAfterWrite(persistFrequency)
      .build<String, String>()

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   init {

      // There's probably a simpler way.
      // Consume the first event per userId for every ${persistFrequency}.
      // However, tried using groupBy and window functions, and found it
      // hard to understand.
      eventSource.userAuthenticated
         .subscribe { event ->
            cachedEvents.get(event.preferredUserName) {
               // The event wasn't present, so emit a seperate
               // event to persist
               eventsToPersist.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST)
               event.preferredUserName
            }

         }

      eventsToPersist.asFlux()
         .subscribe { event ->
            logger.info { "Persisting details for user ${event.preferredUserName}" }
            try {
               val user = vyneUserFromClaims(event.claims)
               val updated = userRepository.upsert(
                  user.id,
                  user.issuer,
                  user.username,
                  user.email,
                  user.profileUrl,
                  user.name
               )
               logger.info { "Upsert returned $updated" }
               userRepository.flush()
            } catch (e: Exception) {
               logger.error(e) { "Failed to write update for user ${event.preferredUserName} - ${e.message}" }
            }

         }
   }
}
