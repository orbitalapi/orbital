package io.vyne.cask.ingest

import arrow.core.extensions.either.applicativeError.raiseError
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.types.ObjectType
import org.postgresql.PGConnection
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
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
   private val hasPrimaryKey = hasPrimaryKey(ingestionStream.type.taxiType as ObjectType)
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
      val writer =
         try
         {
            SimpleRowWriter(table, pgConnection)
         } catch (e: PSQLException) {
            // Apart from DB is down, main reason to be at this point is a schema update
            // resulting a change in the relevant table name. In this case sqlState of the exception will be
            // "42P01" (UNDEFINED_TABLE). We're returning Flux.error which should terminate the socket session
            // on pipeline. Upon termination, pipeline should re-initiate the connection and Cask will re-initialise
            // TypeDbWrapper with correct table name.
            log().error("error in creating row writer for table  ${table.table} Sql State = ${e.sqlState}")
            if (!connection.isClosed) {
               log().error("Closing DB connection for ${table.table}", e)
               // We must close the connection otherwise, we won't return the connection to the connection pool.
               // leading to connection pool exhaustion.
               connection.close()
            }
            return Flux.error(e)
         }
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
