package com.orbitalhq.cockpit.core.scheduler

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.scheduler.ScheduledAnnotation
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import org.quartz.CronTrigger
import org.quartz.Scheduler
import org.quartz.SimpleTrigger
import org.quartz.impl.StdSchedulerFactory
import java.util.*

class ScheduledQuerySynchronizerTest : DescribeSpec({

   describe("Scheduled query sync") {
      lateinit var scheduler: Scheduler
      beforeTest {
         scheduler = testScheduler()
      }
      afterTest {
         if (scheduler != null) {
            scheduler.shutdown(false)
         }
      }
      it("should schedule new queries") {
         val (scheduler, sync) = buildSchedulerAndSync()
         val schema = TaxiSchema.fromStrings(
            ScheduledAnnotation.taxi,
            """
            import com.orbitalhq.scheduler.Scheduled

            @Scheduled(cron = "0/5 * * * * ?")
            query TestQuery {
               find { 1 + 1 }
            }
         """.trimIndent()
         )

         sync.getAllJobKeys().shouldBeEmpty()

         sync.updateScheduledQueries(schema)

         val newJobKeys = sync.getAllJobKeys()
         newJobKeys.shouldHaveSize(1)
         val triggers = scheduler.getTriggersOfJob(newJobKeys.single())
         triggers.shouldHaveSize(1)
         triggers.single().shouldBeInstanceOf<CronTrigger>()
      }

      it("should reschedule changed queries") {
         val (scheduler, sync) = buildSchedulerAndSync()
         val schema = schema(
            """
            import com.orbitalhq.scheduler.Scheduled

            @Scheduled(cron = "0/5 * * * * ?")
            query TestQuery {
               find { 1 + 1 }
            }
         """.trimIndent()
         )

         sync.updateScheduledQueries(schema)
         val triggers = scheduler.getTriggersOfJob(sync.getAllJobKeys().single())
         triggers.shouldHaveSize(1)
         triggers.single().shouldBeInstanceOf<CronTrigger>()

         sync.updateScheduledQueries(
            schema(
               """
            import com.orbitalhq.scheduler.Scheduled

            @Scheduled(fixedRate = "5s")
            query TestQuery {
               find { 1 + 1 }
            }
         """.trimIndent()
            )
         )

         val newKeys = sync.getAllJobKeys()
         newKeys.shouldHaveSize(1)
         val newTriggers = scheduler.getTriggersOfJob(sync.getAllJobKeys().single())
         newTriggers.shouldHaveSize(1)
         newTriggers.single().shouldBeInstanceOf<SimpleTrigger>()
      }

      it("should unschedule removed queries") {
         val (scheduler, sync) = buildSchedulerAndSync()
         val schema = schema(
            """
            import com.orbitalhq.scheduler.Scheduled

            @Scheduled(cron = "0/5 * * * * ?")
            query TestQuery {
               find { 1 + 1 }
            }
         """.trimIndent()
         )

         sync.updateScheduledQueries(schema)
         val triggers = scheduler.getTriggersOfJob(sync.getAllJobKeys().single())
         triggers.shouldHaveSize(1)
         triggers.single().shouldBeInstanceOf<CronTrigger>()

         sync.updateScheduledQueries(
            schema("")
         )

         val newKeys = sync.getAllJobKeys()
         newKeys.shouldBeEmpty()
      }
   }


})

private fun schema(src: String): TaxiSchema = TaxiSchema.fromStrings(
   ScheduledAnnotation.taxi,
   src
)

private fun testScheduler(): Scheduler {
   // This is gross, but it seems the only way to get a fresh scheduler
   // each time.
   // Quartz uses props, instead of actual arguments. :(
   val props = Properties().apply {
      setProperty("org.quartz.scheduler.instanceName", "TestScheduler-" + UUID.randomUUID())
      setProperty("org.quartz.threadPool.threadCount", "1")
      setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
   }
   val factory = StdSchedulerFactory(props)
   val scheduler = factory.scheduler
   scheduler.start()
   return scheduler
}

private fun buildSchedulerAndSync(): Pair<Scheduler, ScheduledQuerySynchronizer> {
//   val scheduler = mock<Scheduler>()
   val scheduler = testScheduler()
//   whenever(scheduler.getJobKeys(GroupMatcher.anyGroup())).thenReturn(emptySet())
   val sync = ScheduledQuerySynchronizer(
      scheduler,
      mock()
   )
   return Pair(scheduler, sync)
}
