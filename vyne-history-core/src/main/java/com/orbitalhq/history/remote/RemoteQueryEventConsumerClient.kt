package com.orbitalhq.history.remote

import com.google.common.cache.CacheBuilder
import com.orbitalhq.history.QueryAnalyticsConfig
import com.orbitalhq.history.QueryResultEventMapper
import com.orbitalhq.history.ResultRowPersistenceStrategy
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.*
import com.orbitalhq.query.history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.Sinks.EmitResult
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

class RemoteQueryEventConsumerClient(
   private val resultRowPersistenceStrategy: ResultRowPersistenceStrategy,
   private val config: QueryAnalyticsConfig,
   private val scope: CoroutineScope
) : QueryEventConsumer, RemoteCallOperationResultHandler {
   private val messageSink = Sinks.many()
      .replay()
      .limit<VyneHistoryRecord>(100, Duration.ofSeconds(10))

   //      .directAllOrNothing<VyneHistoryRecord>()
   private val createdQuerySummaryIds = CacheBuilder.newBuilder()
      .build<String, String>()
   private val emitFailureHandler = EmitFailureHandler { _: SignalType?, emitResult: EmitResult ->
      (emitResult
         == EmitResult.FAIL_NON_SERIALIZED)
   }

   private val objectMapper = Jackson.defaultObjectMapper

   override fun handleEvent(event: QueryEvent) {
      scope.launch {
         when (event) {
            is QueryStartEvent -> persistEvent(event)
            is QueryCompletedEvent -> processQueryCompletedEvent(event)
            is TaxiQlQueryResultEvent -> processQueryResultEvent(event, QueryResultEventMapper.toQuerySummary(event))
            is RestfulQueryResultEvent -> processQueryResultEvent(event, QueryResultEventMapper.toQuerySummary(event))
            is TaxiQlQueryExceptionEvent -> processTaxiQlException(event)
            is QueryFailureEvent -> persistQueryFailureEvent(event)
            is RestfulQueryExceptionEvent -> persistRestfulQueryExceptionEvent(event)
            is StreamingQueryCancelledEvent -> processStreamingQueryCancelledEvent(event)
            else -> {}
         }
      }
   }

   private fun persistEvent(event: QueryStartEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
   }

   private fun createQuerySummaryRecord(queryId: String, factory: () -> QuerySummary) {
      createdQuerySummaryIds.get(queryId) {
         val querySummary = factory()
         emit(querySummary)
         queryId
      }
   }

   private fun processQueryCompletedEvent(event: QueryCompletedEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
      val queryEndEvent = QueryEndEvent(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.COMPLETED,
         event.recordCount
      )
      emit(queryEndEvent)
   }


   private fun persistRestfulQueryExceptionEvent(event: RestfulQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
      val queryEndEvent = QueryEndEvent(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.ERROR,
         event.recordCount,
         event.message
      )
      emit(queryEndEvent)
   }

   private fun persistQueryFailureEvent(event: QueryFailureEvent) {
      emit(
         QueryEndEvent(
            event.queryId,
            Instant.now(),
            QueryResponse.ResponseStatus.ERROR,
            0,
            event.failure.message
         )
      )
   }

   private fun processTaxiQlException(event: TaxiQlQueryExceptionEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
      val queryEndEvent = QueryEndEvent(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.ERROR,
         event.recordCount,
         event.message
      )
      emit(queryEndEvent)
   }

   private fun processStreamingQueryCancelledEvent(event: StreamingQueryCancelledEvent) {
      createQuerySummaryRecord(event.queryId) { QueryResultEventMapper.toQuerySummary(event) }
      val queryEndEvent = QueryEndEvent(
         event.queryId,
         event.timestamp,
         QueryResponse.ResponseStatus.CANCELLED,
         event.recordCount,
         event.message
      )
      emit(queryEndEvent)
   }

   override fun recordResult(operation: OperationResult, queryId: String) {
      val lineageRecords =
         resultRowPersistenceStrategy.createLineageRecords(listOf(operation.asOperationReferenceDataSource()), queryId)
      lineageRecords.forEach { lineageRecord ->
         emit(lineageRecord)
      }

      emit(
         RemoteCallResponse.fromRemoteCall(
            operation.remoteCall,
            queryId,
            objectMapper,
            config.persistRemoteCallResponses
         )
      )
   }

   private fun processQueryResultEvent(event: QueryResultEvent, querySummary: QuerySummary) {
      createQuerySummaryRecord(event.queryId) { querySummary }
      resultRowPersistenceStrategy.extractResultRowAndLineage(event)?.let {
         // queryREsultRow is null if persistResults is set to false
         if (it.queryResultRow != null) {
            emit(it.queryResultRow)
         }

         it.remoteCalls.forEach { remoteCall -> emit(remoteCall) }
         it.lineageRecords.forEach { lineageRecord -> emit(lineageRecord) }
      }
   }

   private fun emit(historyRecord: VyneHistoryRecord) {
      messageSink.emitNext(historyRecord, emitFailureHandler)
      /**
      val emitResult = messageSink.tryEmitNext(historyRecord)
      if (emitResult != EmitResult.OK) {
      logger.error { "Failed to Emit VyneHistoryRecord! result => $emitResult record => $historyRecord" }
      }
       */
   }

   @MessageMapping("analyticsRecords")
   fun queryEvents(): Flux<VyneHistoryRecord> {
      logger.info { "returning analytics records" }
      return messageSink.asFlux()
   }

   fun pushSankeyData(chartRows: List<QuerySankeyChartRow>, queryId: String) {
      this.emit(FlowChartData(chartRows, queryId))
   }
}


