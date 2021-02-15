package io.vyne.queryService

import io.vyne.ExecutableQuery
import io.vyne.RunningQueryStatus
import io.vyne.query.QueryResult
import io.vyne.utils.log
import io.vyne.utils.orElse
import org.springframework.stereotype.Component
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.util.concurrent.CompletableFuture

@Component
class ExecutingQueryRepository(
   val queryHistory: QueryHistory
) {
   private val runningQueries: MutableMap<String, ExecutableQuery> = mutableMapOf()
   private val statusUpdateEmitter = EmitterProcessor.create<RunningQueryStatus>()
   private val statusUpdateSink = statusUpdateEmitter.sink()
   // After upgrading to reactor 2020.x:
//   private val statusUpdateSink = Sinks.many().multicast().onBackpressureBuffer<RunningQueryStatus>()
   fun submit(executableQuery: ExecutableQuery): CompletableFuture<QueryResult> {
      log().info("Adding query ${executableQuery.queryId} to list of running queries")
      this.runningQueries[executableQuery.queryId] = executableQuery
      this.sendStatus(executableQuery)
      executableQuery.currentStatusStream().subscribe { sendStatus(it) }
      executableQuery.result
         .handle { queryResult: QueryResult?, throwable: Throwable? ->
            when {
               queryResult != null -> {
                  log().info("Query ${executableQuery.queryId} has completed, removing from list of running queries")
                  queryHistory.add(VyneQlQueryHistoryRecord(executableQuery.query, queryResult.historyRecord()))
                  removeCompletedQuery(executableQuery)
               }
               throwable != null -> {
                  log().info("Query ${executableQuery.queryId} failed: ${throwable.message.orElse("No message")}")
                  removeCompletedQuery(executableQuery)
               }
            }
         }
      return executableQuery.result
   }

   fun get(queryId: String):ExecutableQuery {
      return runningQueries[queryId] ?: throw NotFoundException("No query with id $queryId was found")
   }

   val statusUpdates: Flux<RunningQueryStatus>
      get() {
         return statusUpdateEmitter
         // after upgrading to reactor 2020.x
//         return statusUpdateSink.asFlux()
      }

   private fun removeCompletedQuery(executableQuery: ExecutableQuery) {
      runningQueries.remove(executableQuery.queryId)
      sendStatus(executableQuery)
   }

   private fun sendStatus(executableQuery: ExecutableQuery) {
      this.sendStatus(executableQuery.currentStatus())
   }

   private fun sendStatus(status: RunningQueryStatus) {
      log().debug("Sending query status update on query ${status.queryId}")
      // after upgrading to reactor 2020.x
//      statusUpdateSink.emitNext(status) { signalType, emitResult ->
//         log().warn("Failed to emit update on executable query ${status.queryId} - $emitResult")
//         false
//      }
      statusUpdateSink.next(status)
   }

   fun list(): List<ExecutableQuery> {
      return runningQueries.values.toList()
   }

   fun stop(queryId: String) {
      val query = this.runningQueries[queryId] ?: throw NotFoundException("No query with id ${queryId} was found")
      log().info("Stopping running query $queryId")
      query.stop()
   }
}



