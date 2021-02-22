package io.vyne.cask.ingest

import io.vyne.cask.batchTimed
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Component
class IngesterFactory(
   final val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
   val caskIngestionErrorProcessor: CaskIngestionErrorProcessor,
   bufferSize:Int = 500,
   bufferTimeout:Duration = Duration.ofSeconds(5),
   scheduler:Scheduler = Schedulers.boundedElastic()
) {
   companion object {
      fun singleThreaded(
         namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
         caskIngestionErrorProcessor: CaskIngestionErrorProcessor,
         bufferSize:Int = 0,
         bufferTimeout:Duration = Duration.ZERO
      ):IngesterFactory = IngesterFactory(namedParameterJdbcTemplate, caskIngestionErrorProcessor, bufferSize = bufferSize, bufferTimeout = bufferTimeout, scheduler = Schedulers.immediate())
   }
   private val bulkInsertIngestorFactory = BulkInsertIngestorFactory(namedParameterJdbcTemplate.jdbcTemplate, bufferSize, bufferTimeout, scheduler)
   private val upsertIngesterFactory = UpsertIngestorFactory(namedParameterJdbcTemplate, bufferSize, bufferTimeout, scheduler)

   @Scheduled(fixedDelay = 5000)
   fun cleanUpCaches() {
      this.bulkInsertIngestorFactory.cleanUpCaches()
      this.upsertIngesterFactory.cleanUpCaches()
   }

   fun create(ingestionStream: IngestionStream): Ingester {
      return batchTimed("Build ingester") {
         Ingester(
            namedParameterJdbcTemplate.jdbcTemplate,
            ingestionStream,
            caskIngestionErrorProcessor.sink(),
            { bulkInsertIngestorFactory.getIngestor(ingestionStream.dbWrapper) },
            { upsertIngesterFactory.getIngestor(ingestionStream.dbWrapper) }
         )
      }

   }
}

data class ConnectionAndWriter(
//   val connection: Connection,
//   val writer: SimpleRowWriter,
   val sink: Sinks.Many<InstanceAttributeSet>
) {
   fun close() {
//      this.writer.close()
//      this.connection.close()
   }
}
