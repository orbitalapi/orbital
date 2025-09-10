package com.orbitalhq.cockpit.core.scheduler

import com.orbitalhq.scheduler.ScheduleConfiguration
import com.orbitalhq.scheduler.ScheduleConfiguration.MisfirePolicy.Default
import com.orbitalhq.scheduler.ScheduleConfiguration.MisfirePolicy.FireImmediately
import com.orbitalhq.scheduler.ScheduleConfiguration.MisfirePolicy.Skip
import org.quartz.CronScheduleBuilder
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.boot.convert.DurationStyle

fun ScheduleConfiguration.MisfirePolicy.applyTo(builder: CronScheduleBuilder): CronScheduleBuilder {
   return when (this) {
      FireImmediately -> builder.withMisfireHandlingInstructionFireAndProceed()
      Skip -> builder.withMisfireHandlingInstructionIgnoreMisfires()
      Default -> builder.withMisfireHandlingInstructionDoNothing()
   }
}

fun ScheduleConfiguration.asQuartzTriggers(): Set<Trigger> {
   return setOfNotNull(
      this.cron?.let { cron ->
         TriggerBuilder.newTrigger()
            .withIdentity("cron")
            .withDescription("Cron schedule: $cron")
            .withPriority(this.priority)
            .withSchedule(
               CronScheduleBuilder.cronSchedule(cron)
                  .let { builder ->
                     misfirePolicy.applyTo(builder)
                  }
            ).build()
      },
      this.fixedRate?.let { fixedRate ->
         TriggerBuilder.newTrigger()
            .withIdentity("fixedRate")
            .withPriority(this.priority)
            .withDescription("Fixed schedule: $fixedRate")
            .withSchedule(
               SimpleScheduleBuilder.simpleSchedule()
                  .withIntervalInMilliseconds(DurationStyle.detectAndParse(fixedRate).toMillis())
                  .let { builder ->
                     if (repeatCount != -1) {
                        builder.withRepeatCount(repeatCount)
                     } else {
                        builder.repeatForever()
                     }
                  }
            ).build()
      }
   )

}
