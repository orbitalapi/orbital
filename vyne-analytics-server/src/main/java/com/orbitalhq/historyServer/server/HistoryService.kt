package com.orbitalhq.historyServer.server

import com.orbitalhq.history.db.*
import com.orbitalhq.query.history.*
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

@Component
class HistoryService(
   queryHistoryRecordRepository: QueryHistoryRecordRepository,
   resultRowRepository: QueryResultRowRepository,
   lineageRecordRepository: LineageRecordRepository,
   remoteCallResponseRepository: RemoteCallResponseRepository,
   sankeyChartRowRepository: QuerySankeyChartRowRepository,
   private val messageSink: Sinks.Many<VyneHistoryRecord>
) : InitializingBean {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private val queryHistoryDao = QueryHistoryDao(
      queryHistoryRecordRepository,
      resultRowRepository,
      lineageRecordRepository,
      remoteCallResponseRepository,
      sankeyChartRowRepository
   )

   override fun afterPropertiesSet() {
      messageSink
         .asFlux()
         .publishOn(Schedulers.boundedElastic())
         .subscribe { vyneHistoryRecord ->
            logger.info { "processing ${vyneHistoryRecord.describe()}" }
            processHistoryRecord(vyneHistoryRecord)
         }
   }

   private fun processHistoryRecord(vyneHistoryRecord: VyneHistoryRecord) {
      when (vyneHistoryRecord) {
         is QuerySummary -> processQuerySummary(vyneHistoryRecord)
         is QueryEndEvent -> processQueryEndEvent(vyneHistoryRecord)
         is LineageRecord -> processLineageRecord(vyneHistoryRecord)
         is QueryResultRow -> processResultRow(vyneHistoryRecord)
         is RemoteCallResponse -> processRemoteCallResponse(vyneHistoryRecord)
         is FlowChartData -> processFlowChartData(vyneHistoryRecord)
         is QuerySankeyChartRow -> {
            // noop - QuerySankeyChartRow comes as part of FlowChartData.
         }
      }
   }

   private fun processFlowChartData(vyneHistoryRecord: FlowChartData) {
      queryHistoryDao.persistFlowChartData(vyneHistoryRecord)
   }

   private fun processRemoteCallResponse(vyneHistoryRecord: RemoteCallResponse) {
      queryHistoryDao.saveRemoteCallResponse(vyneHistoryRecord)
//      queryHistoryDao.upsertRemoteCallResponse(vyneHistoryRecord)
   }

   private fun processResultRow(vyneHistoryRecord: QueryResultRow) {
      queryHistoryDao.saveQueryResultRow(vyneHistoryRecord)
   }

   private fun processLineageRecord(vyneHistoryRecord: LineageRecord) {
      queryHistoryDao.upsertLineageRecord(vyneHistoryRecord)
   }

   private fun processQuerySummary(vyneHistoryRecord: QuerySummary) {
      try {
         queryHistoryDao.saveQuerySummary(vyneHistoryRecord)
      } catch (e: Exception) {
         logger.error(e) { "Error in saving QuerySummary for query Id  ${vyneHistoryRecord.queryId}" }
      }
   }

   private fun processQueryEndEvent(vyneHistoryRecord: QueryEndEvent) {
      try {
         queryHistoryDao.setQueryEnded(
            vyneHistoryRecord.queryId,
            vyneHistoryRecord.endTime,
            vyneHistoryRecord.status,
            vyneHistoryRecord.recordCount,
            vyneHistoryRecord.message
         )
      } catch (e: Exception) {
         logger.error(e) { "Error in saving QueryEndEvent for query Id  ${vyneHistoryRecord.queryId}" }
      }
   }
}
