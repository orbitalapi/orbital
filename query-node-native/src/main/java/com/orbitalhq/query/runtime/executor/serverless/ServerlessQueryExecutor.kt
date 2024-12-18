package com.orbitalhq.query.runtime.executor.serverless

import com.orbitalhq.query.runtime.CompressedQueryResultWrapper
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.executor.QueryExecutor
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class ServerlessQueryExecutor(
   private val executor: QueryExecutor
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun executeQueryReactive(messageCborWrapper: QueryMessageCborWrapper): Mono<CompressedQueryResultWrapper> {
      return Mono.defer {
         val message = messageCborWrapper.message()
         executor.executeQuery(message)
            .map { collectedResult ->
               CompressedQueryResultWrapper.forResult(collectedResult)
            }
            .onErrorResume { e ->
               val errorMessage = "Query execution failed: ${e.message}"
               logger.error(e) { errorMessage }
               Mono.just(CompressedQueryResultWrapper.forError(errorMessage))
            }
      }
   }

   fun executeQuery(messageCborWrapper: QueryMessageCborWrapper): CompressedQueryResultWrapper {
      return executeQueryReactive(messageCborWrapper)
         .subscribeOn(Schedulers.boundedElastic())
         .block()!! // Blocking here on a scheduler that allows it
   }
}
