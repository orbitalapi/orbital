package io.vyne

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContext
import io.vyne.query.QueryResult
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.QualifiedNameAsStringSerializer
import io.vyne.schemas.toVyneQualifiedName
import io.vyne.vyneql.VyneQLQueryString
import io.vyne.vyneql.VyneQlQuery
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.concurrent.CompletableFuture


data class RunningQueryStatus(
   val queryId: String,
   val vyneQlQuery: VyneQLQueryString,
   @JsonSerialize(using = QualifiedNameAsStringSerializer::class)
   val responseTypeName: QualifiedName,
   val completedProjections: Int,
   val estimatedProjectionCount: Int?,
   val startTime: Instant,
   val running: Boolean
)

data class ExecutableQuery(
   @get:JsonIgnore
   val queryContext: QueryContext,
   val query: VyneQLQueryString,
   @get:JsonIgnore
   val parsedQuery: VyneQlQuery,
   @get:JsonIgnore
   val result: CompletableFuture<QueryResult>
) {
   fun resultStream(): Flux<TypedInstance> {
      return queryContext.resultStream
   }

   /**
    * Returns a flux that emits an updated query status each time
    * the result stream emits a new message
    */
   fun currentStatusStream(): Flux<RunningQueryStatus> {
      return queryContext.resultStream.map { currentStatus() }
   }

   fun currentStatus(): RunningQueryStatus {
      val responseTypeName = when {
         parsedQuery.projectedType == null -> parsedQuery.typesToFind[0].type
         parsedQuery.projectedType?.concreteTypeName != null -> parsedQuery.projectedType!!.concreteTypeName!!
         parsedQuery.projectedType?.anonymousTypeDefinition != null -> {
            TODO("Serhat, what do I put here?  How do I find the name of the anonymous type?")
         }
         else -> error("Could not find response type name from VyneQl query")
      }
      return RunningQueryStatus(
         this.queryId,
         this.query,
         responseTypeName.toVyneQualifiedName(),
         this.completedProjections,
         this.estimatedProjectionCount,
         this.startTime,
         running = (!result.isDone && !result.isCancelled && !result.isCompletedExceptionally)
      )
   }

   fun stop(): Boolean {
      // This doesn't actually have any effect on the currently executing query - it just
      // marks the result as cancelled.
      // See https://stackoverflow.com/a/23329340/59015
//      this.result.cancel(true)
      this.queryContext.requestCancel()
      return this.queryContext.isCancelRequested

   }

   val queryId: String = queryContext.queryContextId
   val startTime = queryContext.executionStartTime

   val estimatedProjectionCount: Int?
      get() {
         return queryContext.projectionSize
      }
   val completedProjections: Int
      get() {
         return queryContext.completedProjections
      }
}
