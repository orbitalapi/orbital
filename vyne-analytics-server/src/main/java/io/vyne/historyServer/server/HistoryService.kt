package io.vyne.historyServer.server

import io.vyne.history.QuerySummaryPersister
import io.vyne.history.db.LineageRecordRepository
import io.vyne.history.db.QueryHistoryDao
import io.vyne.history.db.QueryHistoryRecordRepository
import io.vyne.history.db.QueryResultRowRepository
import io.vyne.history.db.QuerySankeyChartRowRepository
import io.vyne.history.db.RemoteCallResponseRepository
import io.vyne.query.history.FlowChartData
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryEndEvent
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySankeyChartRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import io.vyne.query.history.VyneHistoryRecord
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger {}
@Component
class HistoryService(queryHistoryRecordRepository: QueryHistoryRecordRepository,
                     resultRowRepository: QueryResultRowRepository,
                     lineageRecordRepository: LineageRecordRepository,
                     remoteCallResponseRepository: RemoteCallResponseRepository,
                     sankeyChartRowRepository: QuerySankeyChartRowRepository,
                     private val messageSink: Sinks.Many<VyneHistoryRecord>): InitializingBean {
   private val queryHistoryDao = QueryHistoryDao(
      queryHistoryRecordRepository,
      resultRowRepository,
      lineageRecordRepository,
      remoteCallResponseRepository,
      sankeyChartRowRepository)
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
      queryHistoryDao.upsertRemoteCallResponse(vyneHistoryRecord)
   }

   private fun processResultRow(vyneHistoryRecord: QueryResultRow) {
      queryHistoryDao.saveQueryResultRow(vyneHistoryRecord)
   }

   private fun processLineageRecord(vyneHistoryRecord: LineageRecord) {
      queryHistoryDao.upsertLineageRecord(vyneHistoryRecord)
   }

   private fun processQuerySummary(vyneHistoryRecord: QuerySummary) {
      try  {
         queryHistoryDao.saveQuerySummary(vyneHistoryRecord)
      } catch (e: Exception) {
         logger.error(e) { "Error in saving QuerySummary for query Id  ${vyneHistoryRecord.queryId}" }
      }
   }

   private fun processQueryEndEvent(vyneHistoryRecord: QueryEndEvent) {
      try {
         queryHistoryDao.setQueryEnded(vyneHistoryRecord.queryId, vyneHistoryRecord.endTime, vyneHistoryRecord.status, vyneHistoryRecord.recordCount, vyneHistoryRecord.message)
      } catch (e: Exception) {
         logger.error(e) { "Error in saving QueryEndEvent for query Id  ${vyneHistoryRecord.queryId}" }
      }
   }
}
