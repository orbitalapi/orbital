package io.vyne.cask.ingest

import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.schemas.VersionedType
import io.vyne.utils.batchTimed
import io.vyne.utils.log
import lang.taxi.types.ObjectType
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Sinks

data class IngestionStream(
   val type: VersionedType,
   val dbWrapper: TypeDbWrapper,
   val feed: StreamSource
)

typealias InstanceAttributeSetSinkProvider = () -> Sinks.Many<InstanceAttributeSet>

class Ingester(
   private val jdbcTemplate: JdbcTemplate,
   private val ingestionStream: IngestionStream,
   private val ingestionErrorSink: FluxSink<IngestionError>,
   private val bulkCopyInsertSinkProvider: InstanceAttributeSetSinkProvider,
   private val upsertSinkProvider: InstanceAttributeSetSinkProvider
) {

   private val hasPrimaryKey = hasPrimaryKey(ingestionStream.type.taxiType as ObjectType)

   // TODO refactor so that we open/close transaction based on types of messages
   //   1. Message StartTransaction
   //   2. receive InstanceAttributeSet
   //   3. receive InstanceAttributeSet
   //   4. receive InstanceAttributeSet
   //   ...
   //   N receive CommitTransaction

   /**
    * Returns the list of ingested elements.
    * Do not call this in production, as can lead to large ingestion sets being held
    * in memory.
    */
   fun ingestAndCollect(): List<InstanceAttributeSet> {
      return buildIngestionSequence().toList()
   }

   // Returns the count of ingested records.
   // We don't return the actual records, so as to keep data streaming as much as possible,
   // and to minimize memory requirements
   fun ingest(): Int {
      return buildIngestionSequence().count()
   }

   private fun buildIngestionSequence(): Sequence<InstanceAttributeSet> {
      // Here we split the paths that uses jdbcTemplate (for upserting) and pgBulk library.
      // to ensure that we don't initialise pgBulk library path fpr upsert case.
      // Otherwise, pgBulk library grabs an unused connection from the connection pool

      val sink = if (this.hasPrimaryKey) {
         upsertSinkProvider()
      } else {
         bulkCopyInsertSinkProvider()
      }
      return ingestionStream.feed.sequence()
         .map { instanceAttributeSet ->
            sink.emitNext(instanceAttributeSet) { signalType, emitResult ->
               log().error("Failed to persist signal $signalType: $emitResult")
               false // don't retry
            }
            instanceAttributeSet
         }

   }

   private fun ingestThroughUpsert(): Int {
      val sink = upsertSinkProvider()
      return ingestionStream.feed.sequence()
         .map { instanceAttributeSet ->
            sink.emitNext(instanceAttributeSet) { signalType, emitResult ->
               log().error("Failed to persist signal $signalType: $emitResult")
               false // don't retry
            }
            instanceAttributeSet
         }
         .count()
//      val table = ingestionStream.dbWrapper.rowWriterTable
//      return ingestionStream
//         .feed
//         .stream
//         .doOnError {
//            log().error("Closing DB connection for ${table.table}", it)
//            ingestionErrorSink.next(
//               IngestionError.fromThrowable(
//                  it,
//                  this.ingestionStream.feed.messageId,
//                  this.ingestionStream.dbWrapper.type
//               )
//            )
//         }.doOnEach { signal ->
//            signal.get()?.let { instance ->
//               ingestionStream.dbWrapper.upsert(jdbcTemplate, instance)
//            }
//         }.onErrorMap {
//            ingestionErrorSink.next(
//               IngestionError.fromThrowable(
//                  it,
//                  this.ingestionStream.feed.messageId,
//                  this.ingestionStream.dbWrapper.type
//               )
//            )
//            if (it.cause is PSQLException) {
//               it.cause
//            } else {
//               it
//            }
//
//         }
   }

   private fun ingestThroughBulkCopy(): Iterable<InstanceAttributeSet> {
      val sink = bulkCopyInsertSinkProvider()
//      val connection = jdbcTemplate.dataSource!!.connection
//      val pgConnection = connection.unwrap(PGConnection::class.java)
//      val table = ingestionStream.dbWrapper.rowWriterTable
//      val writer =
//         try
//         {
//            SimpleRowWriter(table, pgConnection)
//         } catch (e: PSQLException) {
//            // Apart from DB is down, main reason to be at this point is a schema update
//            // resulting a change in the relevant table name. In this case sqlState of the exception will be
//            // "42P01" (UNDEFINED_TABLE). We're returning Flux.error which should terminate the socket session
//            // on pipeline. Upon termination, pipeline should re-initiate the connection and Cask will re-initialise
//            // TypeDbWrapper with correct table name.
//            log().error("error in creating row writer for table  ${table.table} Sql State = ${e.sqlState}")
//            if (!connection.isClosed) {
//               log().error("Closing DB connection for ${table.table}", e)
//               // We must close the connection otherwise, we won't return the connection to the connection pool.
//               // leading to connection pool exhaustion.
////               connection.close()
//            }
//            ingestionErrorSink.next(IngestionError.fromThrowable(e, this.ingestionStream.feed.messageId, this.ingestionStream.dbWrapper.type))
//            return Flux.error(e)
//         }
//      log().debug("Opening DB connection for ${table.table}")
      return batchTimed("Write records") {
         ingestionStream.feed.sequence()
            .map { instanceAttributeSet ->
               sink.emitNext(instanceAttributeSet) { signalType, emitResult ->
                  log().error("Failed to persist signal $signalType: $emitResult")
                  false // don't retry
               }
               instanceAttributeSet
            }
      }.toList()

//         .doOnError {
//            log().error("Closing DB connection for ${table.table}", it)
////            writer.close()
////            connection.close()
//            ingestionErrorSink.next(IngestionError.fromThrowable(it, this.ingestionStream.feed.messageId, this.ingestionStream.dbWrapper.type))
//         }
//         .doOnComplete {
//            log().info("Closing DB connection for ${table.table}")
////            writer.close()
////            connection.close()
//         }
//         .doOnEach { signal ->
//            signal.get()?.let {
//               sink.emitNext(it, Sinks.EmitFailureHandler { signalType, emitResult ->
//                  log().error("Failed to persist signal $signalType: $emitResult")
//                  false // don't retry
//               })
//            }
//            sink.next(signal.get())
//            signal.get()?.let { instance ->
//               rowWriter.startRow { row ->
//                  ingestionStream.dbWrapper.write(row, instance)
//               }
//            }
//         }.doOnError {
      //invoked when pgbulkinsert throws.
//            if (!connection.isClosed) {
//               log().error("Closing DB connection for ${table.table}", it)
//               connection.close()
//            }
//            ingestionErrorSink.next(IngestionError.fromThrowable(it, this.ingestionStream.feed.messageId, this.ingestionStream.dbWrapper.type))
//   }?
   }


   private fun hasPrimaryKey(type: ObjectType): Boolean {
      return batchTimed("hasPrimaryKey") {
         type.definition?.fields
            ?.flatMap { it -> it.annotations }
            ?.any { a -> a.name == "PrimaryKey" } ?: false
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
