package com.orbitalhq.cockpit.core.scheduler

import com.orbitalhq.VyneProvider
import com.orbitalhq.query.FailedQueryResponse
import com.orbitalhq.query.QueryResponse
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.runtime.core.QueryService
import com.orbitalhq.schema.consumer.SchemaStore
import com.orbitalhq.utils.Ids
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.coroutines.CoroutineContext

class ExecuteScheduledQueryJob(
   private val queryService: QueryService,
   private val schemaStore: SchemaStore
) : Job {
   companion object {
      const val JOB_QUERY_NAME_KEY = "query"
      private val logger = KotlinLogging.logger {}
   }

   override fun execute(context: JobExecutionContext) {
      val schema = schemaStore.schema()
      val queryName = context.jobDetail.jobDataMap.getString(JOB_QUERY_NAME_KEY)
      if (queryName == null) {
         logger.error { "Query job ${context.jobDetail.key} did not provide a query in it's job detail. Ignoring" }
         return
      }
      val query = schema.queries.firstOrNull {
         it.name.parameterizedName == queryName
      }
      if (query == null) {
         logger.warn { "Scheduled query job ${context.jobDetail.key} referenced query $queryName which is not present in this schema. Aborting" }
         return
      }
      val compiledQuery = schema.taxi.query(query.name.parameterizedName)
      val queryId = Ids.id("scheduled-")
      logger.info { "Starting scheduled query ${query.name.parameterizedName} with id $queryId, which fired on trigger ${context.trigger}" }

      // Quartz jobs are supposed to block while they're executing.
      // It's how Quartz detects things like success / failure of jobs,
      // and jobs that are still running
      runBlocking {
         val (queryResults: QueryResponse) = queryService.vyneQLQuery(
            compiledQuery.source,
            null,
            queryId,
            queryId,
         )
         when (queryResults) {
            is QueryResult -> {
               // Collect the results but instantly discard them.
               // It's up to the query to persist etc.
               queryResults.results
                  .onCompletion {
                     logger.info { "Scheduled query $queryId completed" }
                  }
                  .collect { }
            }

            is FailedQueryResponse -> {
               logger.warn { "Scheduled query $queryId failed: ${queryResults.message}" }
            }
         }
      }


   }
}
