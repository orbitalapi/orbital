package io.vyne.pipelines.runner.scheduler

import com.mercateo.test.clock.TestClock
import org.junit.Test
import reactor.test.StepVerifier
import java.time.Duration
import java.time.OffsetDateTime

class CronScheduleTest {

   @Test
   fun `emits signals per cron schedule`() {
      val schedule = "*/10 * * * * *" // every 10 seconds.
      val testClock = TestClock.fixed(OffsetDateTime.now())

      var cronScheduler: CronSchedule? = null
      StepVerifier.withVirtualTime {
         cronScheduler = CronSchedule(
            schedule,
            tickFrequency = Duration.ofSeconds(1),
            clock = testClock
         )
         cronScheduler!!.flux
      }
         .expectSubscription()
         .then {
            // Move the wall clock forward 10 seconds, for the cron job to evaluate
            testClock.fastForward(Duration.ofSeconds(10))
         }
         .thenAwait(Duration.ofSeconds(1)) // Wait for another poll
         .expectNextMatches { tick -> tick == testClock.instant() }
         .expectNoEvent(Duration.ofSeconds(5))
         .then {
            // Move the wall clock forward 10 seconds, for the cron job to evaluate
            testClock.fastForward(Duration.ofSeconds(5))
         }
         .expectNoEvent(Duration.ofSeconds(5))
         .then {
            // Move the wall clock forward 10 seconds, for the cron job to evaluate
            testClock.fastForward(Duration.ofSeconds(10))
         }
         .thenAwait(Duration.ofSeconds(1)) // Wait for another poll
         .expectNextMatches { tick -> tick == testClock.instant() }
         .thenCancel()
         .verify()
   }

}
