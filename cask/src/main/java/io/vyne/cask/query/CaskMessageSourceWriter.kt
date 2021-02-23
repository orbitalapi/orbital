package io.vyne.cask.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.RemovalNotification
import io.vyne.cask.api.ContentType
import io.vyne.cask.ingest.CaskMessage
import io.vyne.schemas.VersionedType
import io.vyne.utils.batchTimed
import io.vyne.utils.log
import org.apache.commons.io.IOUtils
import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.sql.Connection
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

class CaskMessageSourceWriter(
   private val largeObjectDataSource: DataSource,
   private val objectMapper: ObjectMapper = jacksonObjectMapper(),
   bufferSize: Int = 500,
   bufferTimeout: Duration = Duration.ofSeconds(5),
   scheduler: Scheduler = Schedulers.boundedElastic()
) {
   companion object {
      fun singleThreaded(
         largeObjectDataSource: DataSource,
         objectMapper: ObjectMapper = jacksonObjectMapper(),
         bufferSize: Int = 0,
         bufferTimeout: Duration = Duration.ZERO

      ) =
         CaskMessageSourceWriter(largeObjectDataSource, objectMapper, bufferSize, bufferTimeout, Schedulers.immediate())
   }

   private val writerCache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(1, TimeUnit.SECONDS)
      .removalListener<VersionedType, CaskMessageSourceWriterConnection> { notification: RemovalNotification<VersionedType, CaskMessageSourceWriterConnection> ->
         val typeName = notification.key.type.taxiType.qualifiedName
         log().info("Cask message source writer for type $typeName is being closed")
         val stopwatch = Stopwatch.createStarted()
         log().info("Closing message source writer for type $typeName took ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
      }
      .build<VersionedType, CaskMessageSourceWriterConnection>(object :
         CacheLoader<VersionedType, CaskMessageSourceWriterConnection>() {
         override fun load(key: VersionedType): CaskMessageSourceWriterConnection {
            log().info("Building new CaskMessageSourceWriter for type ${key.taxiType.qualifiedName}")

            val sink = Sinks.many().multicast().onBackpressureBuffer<StoreCaskRawMessageRequest>(2500)
            val flux = sink
               .asFlux()
               .publishOn(scheduler)

            val writeCount = AtomicInteger(0)
            val bufferedFlux =
               if (bufferTimeout.isZero) flux.map { listOf(it) } else flux.bufferTimeout(bufferSize, bufferTimeout)
            bufferedFlux
               .subscribe { records ->
                  batchTimed("Writing raw message requests") {
                     val connection = largeObjectDataSource.connection
                     connection.autoCommit = false

                     try {
                        val postgresConnection = connection.unwrap(PGConnection::class.java)

                        records.forEach { storeRawMessageRequest ->
                           val objectId = persistMessageAsLargeObject(postgresConnection, storeRawMessageRequest)
                           writeCaskMessageRecord(connection, objectId, storeRawMessageRequest)
                           writeCount.incrementAndGet()
                        }

                        connection.commit()
                        connection.close()
                     } catch (e: Exception) {
                        log().error("Failed to write batch of messages", e)
                     }
                  }


               }
            return CaskMessageSourceWriterConnection(sink)
         }
      })

   fun writeMessageSource(request: StoreCaskRawMessageRequest) {
      this.writerCache.get(request.versionedType).sink.emitNext(request) { signalType: SignalType, emitResult: Sinks.EmitResult ->
         log().error("Failed to store raw message for request ${request.id}: $emitResult")
         false // don't retry
      }
   }

   private fun writeCaskMessageRecord(
      connection: Connection,
      objectId: Long,
      storeRawMessageRequest: StoreCaskRawMessageRequest
   ) {
      val parametersJson = batchTimed("Convert ingestion parameters to json") {
         objectMapper.writeValueAsString(storeRawMessageRequest.parameters)
      }
      // Don't use spring repository here as:
      // 1. it opens up a new connection
      // 2. large object insertion and corresponding cask_message table insertion must be transactional.
      ////val caskMessage = caskMessageRepository.save(CaskMessage(id, qualifiedTypeName, messageObjectId, insertedAt, contentType, parametersJson))
      val caskMessageInsertInto = connection.prepareStatement(CaskMessage.insertInto)
      caskMessageInsertInto.setString(1, storeRawMessageRequest.id)
      caskMessageInsertInto.setString(2, storeRawMessageRequest.versionedType.fullyQualifiedName)
      caskMessageInsertInto.setLong(3, objectId)
      caskMessageInsertInto.setTimestamp(4, Timestamp.from(Instant.now())) // insertedAt
      caskMessageInsertInto.setString(5, storeRawMessageRequest.contentType.name)
      caskMessageInsertInto.setString(6, parametersJson)
      caskMessageInsertInto.executeUpdate()
   }

   private fun persistMessageAsLargeObject(
      postgresConnection: PGConnection,
      request: StoreCaskRawMessageRequest
   ): Long {
      val largeObjectManager = postgresConnection.largeObjectAPI
      val objectId = largeObjectManager.createLO(LargeObjectManager.READWRITE)
      try {
         val largeObject = largeObjectManager.open(objectId, LargeObjectManager.WRITE)
//         log().info("Streaming message contents to LargeObject API")
         IOUtils.copy(request.inputStream, largeObject.outputStream)
//         log().info("Streaming message contents to LargeObject API completed")
         largeObject.close()
      } catch (exception: Exception) {
         log().error("Exception thrown whilst streaming message content to db", exception)
      }
      return objectId

   }
}

data class CaskMessageSourceWriterConnection(
   val sink: Sinks.Many<StoreCaskRawMessageRequest>
)

data class StoreCaskRawMessageRequest(
   val id: String,
   val versionedType: VersionedType,
   val inputStream: InputStream,
   val contentType: ContentType,
   val parameters: Any
)
