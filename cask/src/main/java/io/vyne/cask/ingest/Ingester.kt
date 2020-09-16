package io.vyne.cask.ingest

import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.timed
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.types.ObjectType
import lang.taxi.types.Type
import org.postgresql.PGConnection
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.util.concurrent.TimeUnit
import kotlin.math.sign

data class IngestionStream(
   val type: VersionedType,
   val dbWrapper: TypeDbWrapper,
   val feed: StreamSource
)

class Ingester(
   private val jdbcTemplate: JdbcTemplate,
   private val ingestionStream: IngestionStream) {

   private val hasPrimaryKey = hasPrimaryKey(ingestionStream.type.taxiType as ObjectType)

   @Deprecated("Remove this in favor of CaskDao")
   fun destroy() {
      jdbcTemplate.execute(ingestionStream.dbWrapper.dropTableStatement)
      TableMetadata.deleteEntry(ingestionStream.type, jdbcTemplate)
   }

   @Deprecated("Remove this in favor of CaskDao")
   fun initialize() {
      timed("Ingester.initialize", true, TimeUnit.MILLISECONDS) {
         jdbcTemplate.execute(TableMetadata.CREATE_TABLE)
         val createTableStatement = ingestionStream.dbWrapper.createTableStatement
         val generatedTableName = createTableStatement.generatedTableName
         log().info("Initializing table $generatedTableName for pipeline for type ${ingestionStream.type.versionedName}")
         jdbcTemplate.execute(createTableStatement.ddlStatement)
         log().info("Table $generatedTableName created")

         log().info("Creating TableMetadata entry for $generatedTableName")
         createTableStatement.metadata.executeInsert(jdbcTemplate)
      }
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
      val writer = SimpleRowWriter(table, pgConnection)
      log().debug("Opening DB connection for ${table.table}")
      return ingestionStream.feed.stream
         .doOnError {
            log().error("Closing DB connection for ${table.table}", it)
            writer.close()
            connection.close()
         }
         .doOnComplete {
            log().info("Closing DB connection for ${table.table}")
            writer.close()
            connection.close()
         }
         .doOnEach { signal ->
            signal.get()?.let { instance ->
               if (this.hasPrimaryKey) {
                  ingestionStream.dbWrapper.upsert(jdbcTemplate,instance)
               } else {
                  writer.startRow { rowWriter ->
                     ingestionStream.dbWrapper.write(rowWriter, instance)
                  }
               }
            }
         }.doOnError {
            //invoked when pgbulkinsert throws.
            if (!connection.isClosed) {
               log().error("Closing DB connection for ${table.table}", it)
               connection.close()
            }
         }
   }

   private fun hasPrimaryKey(type: ObjectType): Boolean {
      return type.definition?.fields
         ?.flatMap { it -> it.annotations }
         ?.any { a -> a.name == "PrimaryKey" } ?: false
   }

   fun getRowCount(): Int {
      val count = jdbcTemplate.queryForObject("SELECT COUNT(*) AS rowcount FROM ${ingestionStream.dbWrapper.tableName}", Int::class.java)!!
      return count
   }
}
