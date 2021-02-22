package io.vyne.cask.ingest

import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.RemovalNotification
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.utils.log
import org.postgresql.PGConnection
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BulkInsertIngestorFactory(
   val jdbcTemplate: JdbcTemplate,
   bufferSize: Int = 500,
   bufferTimeout: Duration = Duration.ofSeconds(5),
   scheduler: Scheduler = Schedulers.boundedElastic()
) {
   fun cleanUpCaches() {
      this.writerCache.cleanUp()
   }

   fun getIngestor(type: TypeDbWrapper): Sinks.Many<InstanceAttributeSet> {
      return writerCache.get(type).sink
   }

   private val writerCache = CacheBuilder
      .newBuilder()
      .expireAfterAccess(1, TimeUnit.SECONDS)
      .removalListener<TypeDbWrapper, ConnectionAndWriter> { notification: RemovalNotification<TypeDbWrapper, ConnectionAndWriter> ->
         log().info("Row writer for type ${notification.key.type.taxiType.qualifiedName} is being closed")
         val stopwatch = Stopwatch.createStarted()
         notification.value.close()
         log().info("Close took ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
      }
      .build<TypeDbWrapper, ConnectionAndWriter>(object : CacheLoader<TypeDbWrapper, ConnectionAndWriter>() {
         override fun load(key: TypeDbWrapper): ConnectionAndWriter {
            log().info("Building new RowWriter for type ${key.type.taxiType.qualifiedName}")

            val sink = Sinks.many().multicast().onBackpressureBuffer<InstanceAttributeSet>()
            val flux = sink
               .asFlux()
               .publishOn(scheduler)

            val writeCount = AtomicInteger(0)
            val bufferedFlux = if (bufferTimeout.isZero) flux.map { listOf(it) } else flux.bufferTimeout(bufferSize,bufferTimeout)
            bufferedFlux
               .subscribe { records ->
                  val stopwatch = Stopwatch.createStarted()
                  val connection = jdbcTemplate.dataSource!!.connection
                  val pgConnection = connection.unwrap(PGConnection::class.java)

                  val table = key.rowWriterTable

                  val writer = SimpleRowWriter(table, pgConnection)

                  records.forEach { attributeSet ->
                     writer.startRow { row ->
                        key.write(row, attributeSet)
                        writeCount.incrementAndGet()
                     }
                  }
                  writer.close()
                  connection.close()
                  log().info(
                     "Flushing ${records.size} records (${writeCount.get()} total) took ${
                        stopwatch.elapsed(
                           TimeUnit.MILLISECONDS
                        )
                     }ms"
                  )
               }
            return ConnectionAndWriter(sink)
         }
      })
}
