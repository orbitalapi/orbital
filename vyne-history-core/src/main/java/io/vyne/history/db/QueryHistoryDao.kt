package io.vyne.history.db

import arrow.core.extensions.list.functorFilter.filter
import com.google.common.base.Stopwatch
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.query.QueryResponse
import io.vyne.query.history.FlowChartData
import io.vyne.query.history.LineageRecord
import io.vyne.query.history.QueryResultRow
import io.vyne.query.history.QuerySummary
import io.vyne.query.history.RemoteCallResponse
import mu.KotlinLogging
import java.sql.SQLIntegrityConstraintViolationException
import java.time.Instant
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Facade to encapsulate history repository actions.
 */
class QueryHistoryDao(
   private val queryHistoryRecordRepository: QueryHistoryRecordRepository,
   private val resultRowRepository: QueryResultRowRepository,
   private val lineageRecordRepository: LineageRecordRepository,
   private val remoteCallResponseRepository: RemoteCallResponseRepository,
   private val sankeyChartRowRepository: QuerySankeyChartRowRepository
) {
   fun persistLineageRecordBatch(lineageRecords: List<LineageRecord>) {
      val sw = Stopwatch.createStarted()
      val existingRecords =
         lineageRecordRepository.findAllById(lineageRecords.map { it.dataSourceId })
            .map { it.dataSourceId }
      val newRecords = lineageRecords.filter { !existingRecords.contains(it.dataSourceId) }
      try {
         lineageRecordRepository.saveAll(newRecords)
      } catch (e: SQLIntegrityConstraintViolationException) {
         logger.warn(e) { "Failed to persist lineage records, as a SQLIntegrityConstraintViolationException was thrown" }
      }
      logger.debug {
         "Persistence batch of ${lineageRecords.size} LineageRecords (filtered to ${newRecords.size}) took ${
            sw.elapsed(
               TimeUnit.MILLISECONDS
            )
         }ms"
      }
   }

   fun upsertLineageRecord(lineageRecord: LineageRecord) {
      try {
         lineageRecordRepository.upsertLineageRecord(
            lineageRecord.dataSourceId,
            lineageRecord.queryId,
            lineageRecord.dataSourceType,
            lineageRecord.dataSourceJson,
            lineageRecord.recordId
         )
      } catch (e: Exception) {
         logger.error(e) { "Error in upserting lineage record for query Id ${lineageRecord.queryId}" }

      }
   }

   fun upsertRemoteCallResponse(remoteCallResponse: RemoteCallResponse) {
      try {
         remoteCallResponseRepository.upsertRemoteCallResponse(
            remoteCallResponse.responseId,
            remoteCallResponse.remoteCallId,
            remoteCallResponse.queryId,
            remoteCallResponse.response
         )
      } catch (e: Exception) {
         logger.error(e) { "error in upserting RemoteCallResponse for query id ${remoteCallResponse.queryId}" }
      }
   }

   fun saveQueryResultRow(queryResult: QueryResultRow) {
      try {
         resultRowRepository.save(queryResult)
      } catch (e: Exception) {
         logger.error(e) { "failed to save QueryResultRow for query id ${queryResult.queryId}" }
      }
   }

   fun saveQueryResultRows(queryResults: List<QueryResultRow>) {
      resultRowRepository.saveAll(queryResults)
   }

   fun saveRemoteCallResponse(remoteCallResponse: RemoteCallResponse) {
      remoteCallResponseRepository.save(remoteCallResponse)
   }

   fun saveRemoteCallResponses(remoteCallResponses: List<RemoteCallResponse>) {
      remoteCallResponseRepository.saveAll(remoteCallResponses)
   }

   fun setQueryEnded(
      queryId: String,
      endTime: Instant,
      status: QueryResponse.ResponseStatus,
      recordCount: Int,
      message: String? = null
   ) {
      queryHistoryRecordRepository.setQueryEnded(queryId, endTime, status, recordCount, message)
   }

   fun saveQuerySummary(querySummary: QuerySummary) {
      queryHistoryRecordRepository.save(querySummary)
   }

   fun persistFlowChartData(flowChartData: FlowChartData) {
      sankeyChartRowRepository.saveAll(flowChartData.data)
   }

   fun persistSankeyChart(queryId: String, sankeyViewBuilder: LineageSankeyViewBuilder) {
      val chartRows = sankeyViewBuilder.asChartRows(queryId)
      sankeyChartRowRepository.saveAll(chartRows)
   }

   fun appendToSankey(instance: TypedInstance, sankeyViewBuilder: LineageSankeyViewBuilder) {
      sankeyViewBuilder.append(instance)
   }

   fun appendOperationResultToSankeyChart(operation: OperationResult, sankeyViewBuilder: LineageSankeyViewBuilder) {
      sankeyViewBuilder.captureOperationResult(operation)
   }
}
