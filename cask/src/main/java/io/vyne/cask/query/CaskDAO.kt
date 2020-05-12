package io.vyne.cask.query

import io.vyne.cask.ingest.QueryView
import io.vyne.schemas.VersionedType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class CaskDAO(private val jdbcTemplate: JdbcTemplate) {
   fun findBy(versionedType: VersionedType, columnName: String, arg: Any): List<Map<String, Any>> {
      val existingMetadaList = QueryView.tableMetadataForVersionedType(versionedType, jdbcTemplate)
      val exactVersionMatch = existingMetadaList.firstOrNull { it.versionHash == versionedType.versionHash }
      val tableName = exactVersionMatch?.tableName ?: existingMetadaList.maxBy { it.timestamp }?.tableName
      return jdbcTemplate.queryForList(findByQuery(tableName!!, columnName), arg)
   }
   companion object {
      fun findByQuery(tableName: String, columnName: String) = "SELECT * FROM $tableName WHERE $columnName = ?"
   }
}
