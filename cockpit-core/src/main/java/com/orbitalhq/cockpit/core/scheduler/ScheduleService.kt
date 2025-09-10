package com.orbitalhq.cockpit.core.scheduler

import com.orbitalhq.scheduler.ScheduleConfiguration
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.ScheduledQueryPublication
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.spring.http.NotFoundException
import org.quartz.Scheduler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ScheduleService(
   private val scheduler: Scheduler,
   private val schemaProvider: SchemaProvider
) {

   @GetMapping("/api/schemas/queries/{queryName}/schedule")
   fun getQuerySchedule(@PathVariable("queryName") queryName: String): ScheduledQueryConfigWithSchedule {
      val schema = schemaProvider.schema
      val query = try {
         schema.taxi.query(queryName)
      } catch (e:Exception) {
         throw NotFoundException("No query named $queryName exists")
      }

      val savedQuery = schema.queries.first { it.name.parameterizedName == queryName }
      val scheduleConfig = savedQuery.publications.filterIsInstance<ScheduledQueryPublication>()
         .firstOrNull()
         ?.config
         ?: throw BadRequestException("Query $queryName is not a scheduled query")

      val existingTriggers = scheduler.getTriggersOfJob(query.jobKey)
      val scheduledTriggers = existingTriggers.map { trigger ->
         ScheduledTrigger(trigger.description ?: trigger::class.simpleName!!,trigger.previousFireTime?.toInstant(),  trigger.nextFireTime?.toInstant())
      }
      return ScheduledQueryConfigWithSchedule(
         scheduleConfig,
         scheduledTriggers
      )
   }
}


data class ScheduledQueryConfigWithSchedule(
   val scheduleConfiguration: ScheduleConfiguration,
   val triggers: List<ScheduledTrigger>
)

data class ScheduledTrigger(val description: String, val lastTriggerDate: Instant?, val nextTriggerDate: Instant?)
