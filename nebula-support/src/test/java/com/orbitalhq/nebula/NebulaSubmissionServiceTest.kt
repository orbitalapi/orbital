package com.orbitalhq.nebula

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.orbitalhq.VersionedSource
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.kotlin.test.test
import reactor.test.StepVerifier
import java.time.Duration

class NebulaSubmissionServiceTest {

   private lateinit var nebulaApi: NebulaApi
   private lateinit var schemaWatcher: NebulaSchemaWatcher
   private lateinit var envVariableSource: NebulaEnvVariableSource
   private lateinit var nebulaService: NebulaSubmissionService

   private lateinit var stacksUpdatedSink:Sinks.Many<NebulaStacksChangedEvent>

   @BeforeEach
   fun setup() {
      // Mock dependencies
      nebulaApi = mock {}
      schemaWatcher = mock {}
      envVariableSource = mock {}

      stacksUpdatedSink = Sinks.many().unicast().onBackpressureError()
      whenever(schemaWatcher.stacksUpdated).thenReturn(stacksUpdatedSink.asFlux())
      // Initialize the service with mocked dependencies
      nebulaService = NebulaSubmissionService(nebulaApi, schemaWatcher, envVariableSource)
   }

   @Test
   fun `when update is emitted, it's submitted to nebulaApi`() {
      val added = mapOf("stack1" to VersionedSource.sourceOnly("stack {}"))
      val event = NebulaStacksChangedEvent(added, emptyMap(), emptyMap())

      val nebulaStack = mapOf<String, Map<String, ComponentInfo>>("stack1" to emptyMap())
      nebulaService.stateUpdates.test()
         .expectSubscription()
         .then {
            // Mock the update API call to return success
            whenever(nebulaApi.updateStack(anyString(), anyString())).thenReturn(Mono.just("stack1"))
            whenever(nebulaApi.getStacks()).thenReturn(Mono.just(nebulaStack))
            stacksUpdatedSink.emitNext(event, Sinks.EmitFailureHandler.FAIL_FAST)
         }.expectNextMatches { event ->
            event.hasError.shouldBeFalse()
            event.stacks.shouldBe(nebulaStack)
            true
         }
         .then {
            // verify the env vars were updated
            verify(envVariableSource).updateEnvVariables(nebulaStack)
         }
         .thenCancel()
         .verify(Duration.ofSeconds(5))
   }

   @Test
   fun `when submission fails, then retry`() {
      val added = mapOf("stack1" to VersionedSource.sourceOnly("stack {}"))
      val updateEvent = NebulaStacksChangedEvent(added, emptyMap(), emptyMap())

      // Mock failure for the first two attempts and success on the third attempt
      whenever(nebulaApi.updateStack(anyString(), anyString()))
         .thenReturn(Mono.error(RuntimeException("Submission failed")))
         .thenReturn(Mono.error(RuntimeException("Submission failed again")))
         .thenReturn(Mono.just("stack1"))

      val nebulaStack = mapOf<String, Map<String, ComponentInfo>>("stack1" to emptyMap())
      whenever(nebulaApi.getStacks()).thenReturn(Mono.just(nebulaStack))
      StepVerifier.withVirtualTime { nebulaService.stateUpdates }
//      StepVerifier.create(nebulaService.stateUpdates)
         .expectSubscription()
         .then {
            stacksUpdatedSink.emitNext(updateEvent, Sinks.EmitFailureHandler.FAIL_FAST)
         }.expectNextMatches { stateEvent ->
            stateEvent.hasError.shouldBeTrue()
            stateEvent.hasPendingUpdates.shouldBeTrue()
            true
         }
         .thenAwait(Duration.ofSeconds(5))
         // Second failure after the first retry
         .expectNextMatches { stateEvent ->
            stateEvent.hasError.shouldBeTrue()
            stateEvent.hasPendingUpdates.shouldBeTrue()
            true
         }
         .thenAwait(Duration.ofSeconds(5))
         // Finally, the successful update after the third attempt
         .expectNextMatches { stateEvent ->
            stateEvent.hasError.shouldBeFalse() // Successful update
            stateEvent.stacks.shouldBe(nebulaStack)
            stateEvent.hasPendingUpdates.shouldBeFalse() // No pending updates after success
            true
         }
         .thenCancel()
         .verify(Duration.ofSeconds(60))

      // Verify that the update API was retried 3 times
      verify(nebulaApi, times(3)).updateStack(anyString(), anyString())
   }


}
