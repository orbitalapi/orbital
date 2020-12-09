package io.vyne.queryService

import io.vyne.ExecutableQuery
import io.vyne.utils.log
import org.springframework.stereotype.Component

@Component
class ExecutingQueryRepository {
   private val runningQueries: MutableMap<String, ExecutableQuery> = mutableMapOf()

   fun submit(executableQuery: ExecutableQuery) {
      log().info("Adding query ${executableQuery.queryId} to list of running queries")
      this.runningQueries[executableQuery.queryId] = executableQuery
      executableQuery.result
         .thenRun {
            log().info("Query ${executableQuery.queryId} has completed, removing from list of running queries")
            runningQueries.remove(executableQuery.queryId)
         }
   }

   fun list():List<ExecutableQuery> {
      return runningQueries.values.toList()
   }
}


