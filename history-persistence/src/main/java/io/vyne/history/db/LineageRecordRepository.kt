package io.vyne.history.db

import io.vyne.query.history.LineageRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface LineageRecordRepository : JpaRepository<LineageRecord, String> {

   fun findByQueryIdAndDataSourceId(queryId: String, dataSourceId: String): Optional<LineageRecord>

   @Transactional
   fun findAllByQueryIdAndDataSourceType(queryId: String, dataSourceType: String): List<LineageRecord>

   fun findAllByDataSourceId(dataSourceId: String): List<LineageRecord>

   @Transactional
   fun findAllByQueryId(queryId: String): List<LineageRecord>

   @Modifying
   @Query(
      value = "INSERT INTO LINEAGE_RECORD (record_id, data_source_id, query_id, data_source_type, data_source_json) VALUES(:recordId, :dataSourceId, :queryId, :dataSourceType, :dataSourceJson) ON CONFLICT DO NOTHING",
      nativeQuery = true
   )
   @Transactional
   fun upsertLineageRecord(
      @Param("recordId") recordId: String,
      @Param("dataSourceId") dataSourceId: String,
      @Param("queryId") queryId: String,
      @Param("dataSourceType") dataSourceType: String,
      @Param("dataSourceJson") dataSourceJson: String
   )
}
