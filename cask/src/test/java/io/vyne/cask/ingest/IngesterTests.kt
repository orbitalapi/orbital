package io.vyne.cask.ingest

import com.nhaarman.mockito_kotlin.mock
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.vyne.cask.MessageIds
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.schemas.fqn
import org.apache.commons.csv.CSVFormat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.postgresql.PGConnection
import org.postgresql.copy.CopyIn
import org.postgresql.copy.CopyManager
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.UnicastProcessor
import java.io.File
import java.io.InputStream
import java.sql.Connection
import javax.sql.DataSource

class IngesterTests {
   @Rule
   @JvmField
   val folder = TemporaryFolder()
   val jdbcTemplate: JdbcTemplate = mock()
   val dataSource: DataSource = mock()
   val connection: Connection = mock()
   val pgConnection: PGConnection = mock()
   val copyManager: CopyManager = mock()
   val copyIn: CopyIn = mock()
   val ingestionErrorRepository: IngestionErrorRepository = mock()
   val ingestionErrorProcessor = CaskIngestionErrorProcessor(ingestionErrorRepository)
   val ingestionErrorSink = ingestionErrorProcessor.sink()

   val schema = CoinbaseOrderSchema.schemaV1
   val type = schema.versionedType("OrderWindowSummary".fqn())

   @Before
   fun setUp() {
      whenever(jdbcTemplate.dataSource).thenReturn(dataSource)
      whenever(dataSource.connection).thenReturn(connection)
      whenever(connection.unwrap(PGConnection::class.java)).thenReturn(pgConnection)
      whenever(pgConnection.copyAPI).thenReturn(copyManager)
      whenever(copyManager.copyIn(any())).thenReturn(copyIn)
   }

   @Test
   fun `Ingester closes underlying DB connection properly when ingested successfully`() {
      //given
      val input: Flux<InputStream> = Flux.just(File( Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()).inputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = ingestionErrorProcessor)
      val pipeline = IngestionStream(
         type,
         TypeDbWrapper(type, schema),
         pipelineSource)
     val ingester = Ingester(jdbcTemplate, pipeline, ingestionErrorSink, CaskMutationDispatcher(), SimpleMeterRegistry())

      // when
      ingester.ingest().collectList().block()
      // then
      verify(connection, times(1)).close()
   }

   @Test
   fun `Ingester closes underlying DB connection properly when ingestestion fails`() {
      //given
      val input: Flux<InputStream> = Flux.just(File( Resources.getResource("Coinbase_BTCUSD_invalid.csv").toURI()).inputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = ingestionErrorProcessor)
      val pipeline = IngestionStream(
         type,
         TypeDbWrapper(type, schema),
         pipelineSource)
      val ingester = Ingester(jdbcTemplate, pipeline, ingestionErrorSink, CaskMutationDispatcher(), SimpleMeterRegistry())
      // when
      try {
         ingester.ingest().collectList().block()
      } catch(e: Exception) {}
      // then
      verify(connection, times(2)).close()
   }

   @Test
   fun `Ingester closes underlying DB connection properly when pgbulkinsert throws`() {
      //given
      val input: Flux<InputStream> = Flux.just(File( Resources.getResource("Coinbase_BTCUSD_1h.csv").toURI()).inputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = ingestionErrorProcessor)
      whenever(copyIn.flushCopy()).thenThrow(ArithmeticException("Negative Exponent"))
      val pipeline = IngestionStream(
         type,
         TypeDbWrapper(type, schema),
         pipelineSource)
      val ingester = Ingester(jdbcTemplate, pipeline, ingestionErrorSink, CaskMutationDispatcher(), SimpleMeterRegistry())

      // when
      try {
         ingester.ingest().collectList().block()
      } catch(e: Exception) {}
      // then
      verify(connection, times(1)).close()
   }
}
