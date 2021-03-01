package io.vyne.cask.ingest

import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.RemovalNotification
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.metrics.Meters
import io.vyne.utils.log
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class UpsertIngestorFactory(
   val namedJdbcTemplate: NamedParameterJdbcTemplate,
   bufferSize: Int = 500,
   bufferTimeout: Duration = Duration.ofSeconds(5),
   scheduler: Scheduler = Schedulers.boundedElastic(),
   meterRegistry: MeterRegistry? = null
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
         override fun load(typeDbWrapper: TypeDbWrapper): ConnectionAndWriter {
            val timer = meterRegistry?.timer(Meters.UPSERT_PERSIST)
            val upsertCounter = meterRegistry?.counter(Meters.PERSISTED_COUNT)
            val sink = Sinks
               .unsafe()
               .many()
               .multicast()
               // TODO Check back pressure size.
               .onBackpressureBuffer<InstanceAttributeSet>(Int.MAX_VALUE)

            val flux = sink
               .asFlux()
               .publishOn(scheduler)

            val writeCount = AtomicInteger(0)
            val upsertStatement = typeDbWrapper.upsertStatement
            val bufferedFlux = if (bufferTimeout.isZero) flux.map { listOf(it) } else flux.bufferTimeout(bufferSize,bufferTimeout)
            bufferedFlux
               .subscribe { records ->
                  val stopwatch = Stopwatch.createStarted()
                  val parameterSources: Array<SqlParameterSource> = records.map {
                     writeCount.incrementAndGet()
                     upsertStatement.toParameterSource(it)
                  }.toTypedArray()
                  namedJdbcTemplate.batchUpdate(
                     upsertStatement.sqlStatement,
                     parameterSources
                  )
                  timer?.let {
                     timer.record(stopwatch.elapsed())
                  }
                  upsertCounter?.increment(records.size.toDouble())
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
