package io.vyne.cask.ingest

import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.schemas.VersionedType
import io.vyne.utils.log
import lang.taxi.types.ObjectType
import mu.KotlinLogging
import org.postgresql.PGConnection
import org.postgresql.util.PSQLException
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

data class IngestionStream(
   val type: VersionedType,
   val dbWrapper: TypeDbWrapper,
   val feed: StreamSource
)

class Ingester(
   private val jdbcTemplate: JdbcTemplate,
   private val ingestionStream: IngestionStream,
   private val ingestionErrorSink: FluxSink<IngestionError>,
   private val caskMutationDispatcher: CaskChangeMutationDispatcher
) {
   private val hasPrimaryKey = TaxiAnnotationHelper.hasPrimaryKey(ingestionStream.type.taxiType as ObjectType)
   // TODO refactor so that we open/close transaction based on types of messages
   //   1. Message StartTransaction
   //   2. receive InstanceAttributeSet
   //   3. receive InstanceAttributeSet
   //   4. receive InstanceAttributeSet
   //   ...
   //   N receive CommitTransaction
   fun ingest(): Flux<CaskEntityMutatingMessage> {
      // Here we split the paths that uses jdbcTemplate (for upserting) and pgBulk library.
      // to ensure that we don't initialise pgBulk library path fpr upsert case.
      // Otherwise, pgBulk library grabs an unused connection from the connection pool
      return if (this.hasPrimaryKey) {
         this.ingestThroughUpsert()
      } else {
         this.ingestThroughBulkCopy()
      }
   }

   private fun ingestThroughUpsert(): Flux<CaskEntityMutatingMessage> {
      val table = ingestionStream.dbWrapper.rowWriterTable
      return ingestionStream
         .feed
         .stream
         .doOnError {
            log().error("Closing DB connection for ${table.table}", it)
            ingestionErrorSink.next(
               IngestionError.fromThrowable(
                  it,
                  this.ingestionStream.feed.messageId,
                  this.ingestionStream.dbWrapper.type
               )
            )
         }.map { instance ->
            ingestionStream.dbWrapper.upsert(jdbcTemplate, instance)
         }.onErrorMap {
            ingestionErrorSink.next(
               IngestionError.fromThrowable(
                  it,
                  this.ingestionStream.feed.messageId,
                  this.ingestionStream.dbWrapper.type
               )
            )
            if (it.cause is PSQLException) {
               it.cause
            } else {
               it
            }
         }
   }

   private fun ingestThroughBulkCopy(): Flux<CaskEntityMutatingMessage> {
      val connection = jdbcTemplate.dataSource!!.connection
      val pgConnection = connection.unwrap(PGConnection::class.java)
      val table = ingestionStream.dbWrapper.rowWriterTable
      val writer =
         try {
            SimpleRowWriter(table, pgConnection)
         } catch (e: PSQLException) {
            // Apart from DB is down, main reason to be at this point is a schema update
            // resulting a change in the relevant table name. In this case sqlState of the exception will be
            // "42P01" (UNDEFINED_TABLE). We're returning Flux.error which should terminate the socket session
            // on pipeline. Upon termination, pipeline should re-initiate the connection and Cask will re-initialise
            // TypeDbWrapper with correct table name.
            log().error("error in creating row writer for table  ${table.table} Sql State = ${e.sqlState}")
            if (!connection.isClosed) {
               logger.error {"Closing DB connection for ${table.table}" }
               // We must close the connection otherwise, we won't return the connection to the connection pool.
               // leading to connection pool exhaustion.
               connection.close()
            }
            ingestionErrorSink.next(
               IngestionError.fromThrowable(
                  e,
                  this.ingestionStream.feed.messageId,
                  this.ingestionStream.dbWrapper.type
               )
            )
            return Flux.error(e)
         }
      log().debug("Opening DB connection for ${table.table}")
      return ingestionStream.feed.stream
         .doOnError {
            logger.error {"Closing DB connection for ${table.table} ${it.message}" }
            writer.close()
            connection.close()
            ingestionErrorSink.next(
               IngestionError.fromThrowable(
                  it,
                  this.ingestionStream.feed.messageId,
                  this.ingestionStream.dbWrapper.type
               )
            )
         }
         .doOnComplete {
            try { writer.close() } catch (exception: Exception) { logger.error { "Unable to close writer ${exception.message}" } }
            try { connection.close() } catch (exception: Exception) { logger.error { "Unable to close connection ${exception.message}" } }
         }
         .doOnError {
            //invoked when pgbulkinsert throws.
            if (!connection.isClosed) {
               logger.error {"Closing DB connection for ${table.table}"}
               connection.close()
            }
            ingestionErrorSink.next(IngestionError.fromThrowable(it, this.ingestionStream.feed.messageId, this.ingestionStream.dbWrapper.type))
         }
         .switchMap { instance ->
            Mono.create { sink ->

               writer.startRow { rowWriter ->

                  try {
                     val caskMutationMessage = ingestionStream.dbWrapper.write(rowWriter, instance)
                     caskMutationDispatcher.accept(caskMutationMessage)
                     sink.success(caskMutationMessage)

                  } catch (e: Exception) {
                     if (!connection.isClosed) {
                        logger.error {"Closing DB connection for ${table.table} because of exception ${e.message}" }
                        connection.close()
                     }
                     ingestionErrorSink.next(
                        IngestionError.fromThrowable(
                           e,
                           this.ingestionStream.feed.messageId,
                           this.ingestionStream.dbWrapper.type
                        )
                     )
                     sink.error(e)
                  }
               }
            }
         }
   }

   fun getRowCount(): Int {
      val count = jdbcTemplate.queryForObject(
         "SELECT COUNT(*) AS rowcount FROM ${ingestionStream.dbWrapper.tableName}",
         Int::class.java
      )!!
      return count
   }
}
