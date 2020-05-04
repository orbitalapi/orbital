package io.vyne.cask.ingest

import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import org.postgresql.PGConnection
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux

data class Pipeline(
   val type: VersionedType,
   val dbWrapper: TypeDbWrapper,
   val feed: PipelineSource
)

class Ingester(private val jdbcTemplate: JdbcTemplate, private val pipeline: Pipeline) {

    fun destroy() {
        jdbcTemplate.execute(pipeline.dbWrapper.dropTableStatement)
        TableMetadata.deleteEntry(pipeline.type, jdbcTemplate)
    }

    fun initialize() {
        jdbcTemplate.execute(TableMetadata.CREATE_TABLE)
        val createTableStatement = pipeline.dbWrapper.createTableStatement
        val generatedTableName = createTableStatement.generatedTableName
        log().info("Initializing table $generatedTableName for pipeline for type ${pipeline.type.versionedName}")
        jdbcTemplate.execute(createTableStatement.ddlStatement)
        log().info("Table $generatedTableName created")

        log().info("Creating TableMetadata entry for $generatedTableName")
        createTableStatement.metadata.executeInsert(jdbcTemplate)
    }

    fun ingest(): Flux<InstanceAttributeSet> {
        val connection = jdbcTemplate.dataSource!!.connection
        val pgConnection = connection.unwrap(PGConnection::class.java)
        val table = pipeline.dbWrapper.rowWriterTable
        val writer = SimpleRowWriter(table)
        writer.open(pgConnection)
        return pipeline.feed.stream
                .doOnComplete {
                    writer.close()
                    connection.close()
                }
                .doOnEach { signal ->
                    signal.get()?.let { instance ->
                        writer.startRow { rowWriter ->
                            pipeline.dbWrapper.write(rowWriter, instance)
                        }
                    }

                }

    }

    fun getRowCount(): Int {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) AS rowcount FROM ${pipeline.dbWrapper.tableName}", Int::class.java)!!
        return count
    }
}
