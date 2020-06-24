package io.vyne.cask.ingest

import com.google.common.annotations.VisibleForTesting
import io.vyne.cask.ddl.TableMetadata
import io.vyne.schemas.VersionedType
import io.vyne.schemas.taxi.TaxiSchema
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.nio.file.Paths
import java.sql.Connection
import java.sql.ResultSet

class QueryView(private val jdbcTemplate: JdbcTemplate) {
    fun prepare(versionedType: VersionedType, schema: TaxiSchema): Flux<InstanceAttributeSet> {
        return prepare(getQueryStrategy(versionedType), schema)
    }

    fun prepare(strategy: DataQuerySpec, schema: TaxiSchema): Flux<InstanceAttributeSet> {
        return when (strategy) {
            is UpgradeDataSourceSpec -> DataSourceUpgrader(schema, strategy, jdbcTemplate).execute()
            else -> Flux.empty()
        }
    }

    @VisibleForTesting
    fun destroy(strategy: DataQuerySpec, schema: TaxiSchema) {
        when (strategy) {
            is UpgradeDataSourceSpec ->  DataSourceUpgrader(schema, strategy, jdbcTemplate).destroy()
            else -> TODO()
        }
    }

    fun getQueryStrategy(type: VersionedType): DataQuerySpec {
        val existingDataSources = tableMetadataForVersionedType(type, jdbcTemplate)
        if (existingDataSources.isEmpty()) {
            error("No data exists for type ${type.fullyQualifiedName}")
        }
        val exactVersionMatches = existingDataSources.filter { it.versionHash == type.versionHash }
        if (exactVersionMatches.isNotEmpty()) {
            return TableQuerySpec(exactVersionMatches, type)
        }
        val latestDataSource = existingDataSources.maxBy { it.timestamp }!!
        return UpgradeDataSourceSpec(latestDataSource, type)
    }
   companion object {
      fun tableMetadataForVersionedType(type: VersionedType, jdbcTemplate: JdbcTemplate): List<TableMetadata> = jdbcTemplate.query(
         { con: Connection ->
            con.prepareStatement("SELECT * from ${TableMetadata.TABLE_NAME} where qualifiedTypeName = ?")
               .apply { setString(1, type.fullyQualifiedName) }
         }

      ) { rs: ResultSet, _: Int ->
         TableMetadata(
            tableName = rs.getString(1),
            qualifiedTypeName = rs.getString(2),
            versionHash = rs.getString(3),
            sourceSchemaIds = (rs.getArray(4).array as Array<String>).toList(),
            sources = (rs.getArray(5).array as Array<String>).toList(),
            timestamp = rs.getTimestamp(6).toInstant(),
            readCachePath = Paths.get(rs.getString(7)),
            deltaAgainstTableName = rs.getString(8)
         )
      }
   }
}

interface DataQuerySpec
data class UpgradeDataSourceSpec(val source: TableMetadata, val targetType: VersionedType) : DataQuerySpec
data class TableQuerySpec(val tables: List<TableMetadata>, val targetType: VersionedType) : DataQuerySpec
