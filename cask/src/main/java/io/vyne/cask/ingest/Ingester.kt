package io.vyne.cask.ingest

import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import org.postgresql.PGConnection
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux

data class IngestionStream(
   val type: VersionedType,
   val dbWrapper: TypeDbWrapper,
   val feed: StreamSource
)

class Ingester(
   private val jdbcTemplate: JdbcTemplate,
   private val ingestionStream: IngestionStream) {

    fun destroy() {
        jdbcTemplate.execute(ingestionStream.dbWrapper.dropTableStatement)
        TableMetadata.deleteEntry(ingestionStream.type, jdbcTemplate)
    }

    fun initialize() {
        jdbcTemplate.execute(TableMetadata.CREATE_TABLE)
        val createTableStatement = ingestionStream.dbWrapper.createTableStatement
        val generatedTableName = createTableStatement.generatedTableName
        log().info("Initializing table $generatedTableName for pipeline for type ${ingestionStream.type.versionedName}")
        jdbcTemplate.execute(createTableStatement.ddlStatement)
        log().info("Table $generatedTableName created")

        log().info("Creating TableMetadata entry for $generatedTableName")
        createTableStatement.metadata.executeInsert(jdbcTemplate)
    }

    // TODO refactor so that we open/close transaction based on types of messages
    //   1. Message StartTransaction
    //   2. receive InstanceAttributeSet
    //   3. receive InstanceAttributeSet
    //   4. receive InstanceAttributeSet
    //   ...
    //   N receive CommitTransaction

    fun ingest(): Flux<InstanceAttributeSet> {
        val connection = jdbcTemplate.dataSource!!.connection
        val pgConnection = connection.unwrap(PGConnection::class.java)
        val table = ingestionStream.dbWrapper.rowWriterTable
        val writer = SimpleRowWriter(table)
        writer.open(pgConnection)
        return ingestionStream.feed.stream
                .doOnComplete {
                    writer.close()
                    connection.close()
                }
                .doOnEach { signal ->
                    signal.get()?.let { instance ->
                        writer.startRow { rowWriter ->
                            ingestionStream.dbWrapper.write(rowWriter, instance)
                        }
                    }

                }

    }

    fun getRowCount(): Int {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) AS rowcount FROM ${ingestionStream.dbWrapper.tableName}", Int::class.java)!!
        return count
    }
}
