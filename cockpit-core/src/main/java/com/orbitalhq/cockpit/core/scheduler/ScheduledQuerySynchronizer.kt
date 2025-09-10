package com.orbitalhq.cockpit.core.scheduler

import arrow.core.getOrElse
import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.scheduler.ScheduleConfiguration
import com.orbitalhq.scheduler.ScheduledAnnotation
import com.orbitalhq.schema.consumer.SchemaChangedEventProvider
import com.orbitalhq.schemas.Schema
import jakarta.annotation.PostConstruct
import lang.taxi.TaxiDocument
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.Annotation
import lang.taxi.types.annotationOrInheritedAnnotation
import mu.KotlinLogging
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux

/**
 * Responsible for listening to schema changes,
 * and updating scheduled jobs to the Quartz scheduler
 */
@Component
class ScheduledQuerySynchronizer(
   private val scheduler: Scheduler,
   private val schemaChangeNotifier: SchemaChangedEventProvider
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PostConstruct
   fun listenForSchemaChanges() {
      schemaChangeNotifier.schemaChanged
         .toFlux()
         .subscribeOn(Schedulers.boundedElastic())
         .subscribe { event ->
            logger.info { "Schema updated - synchronizing scheduled queries" }
            updateScheduledQueries(event.newSchemaSet.schema)
         }
   }

   fun getAllJobKeys() = scheduler.getJobKeys(GroupMatcher.anyGroup())
   @VisibleForTesting
   fun updateScheduledQueries(schema: Schema) {
      val taxi = schema.taxi
      if (!taxi.containsType(ScheduledAnnotation.ScheduledTypeName.parameterizedName)) {
         logger.warn { "Scheduled annotation not found in schema - not attempting to update scheduled queries" }
         return
      }
      val scheduledQueries = getScheduledQueriesFromSchema(taxi)
      val desiredJobKeys = scheduledQueries.map { it.first.jobKey }
      val existingJobs = getAllJobKeys()

      val jobsToRemove = existingJobs.filter { it !in desiredJobKeys }
      removeJobs(jobsToRemove)

      addAndUpdateQueries(scheduledQueries)
   }

   private fun addAndUpdateQueries(scheduledQueries: List<Pair<TaxiQlQuery, Annotation>>) {
      scheduledQueries.forEach { (query, annotation) ->
         val jobKey = query.jobKey
         val scheduleConfiguration = ScheduleConfiguration.fromAnnotation(annotation)
            .getOrElse {
               error("TODO : Handle parsing failure of schedule")
            }
         val existingTriggers = scheduler.getTriggersOfJob(jobKey)

         val jobDetail = JobBuilder.newJob(ExecuteScheduledQueryJob::class.java)
            .withIdentity(jobKey)
            .usingJobData(ExecuteScheduledQueryJob.JOB_QUERY_NAME_KEY, query.qualifiedName)
            .build()
         if (existingTriggers.isEmpty()) {
            logger.info { "Scheduling new scheduled query: ${query.qualifiedName} with config: $scheduleConfiguration" }
            scheduler.scheduleJob(jobDetail, scheduleConfiguration.asQuartzTriggers(), true)
         } else {
            if (existingTriggers.toSet() != scheduleConfiguration.asQuartzTriggers()) {
               logger.info { "Updating existing scheduled query: ${query.qualifiedName} with config: $scheduleConfiguration" }
               scheduler.unscheduleJobs(existingTriggers.map { it.key })
               scheduler.scheduleJob(jobDetail, scheduleConfiguration.asQuartzTriggers(), true)
            }
         }
      }
   }


   private fun removeJobs(jobsToRemove: List<JobKey>) {
      jobsToRemove.forEach { jobKey ->
         scheduler.deleteJob(jobKey)
         logger.info { "Removed scheduled query $jobKey" }
      }
   }

   private fun getScheduledQueriesFromSchema(
      schema: TaxiDocument,
   ): List<Pair<TaxiQlQuery, Annotation>> {
      val scheduledAnnotation = schema.annotation(ScheduledAnnotation.ScheduledTypeName.parameterizedName)

      val scheduledQueries = schema.queries
         .mapNotNull { query ->
            val annotations = query.annotationOrInheritedAnnotation(scheduledAnnotation)
            when {
               annotations.isEmpty() -> null
               annotations.size > 1 -> {
                  logger.warn { "Query ${query.qualifiedName} has multiple Scheduled annotations - this is invalid. Ignoring this query" }
                  null
               }

               else -> query to annotations.single()
            }
         }
      return scheduledQueries
   }
}

internal val TaxiQlQuery.jobKey
   get() = JobKey(this.qualifiedName)

