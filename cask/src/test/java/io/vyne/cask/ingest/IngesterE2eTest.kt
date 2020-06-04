package io.vyne.cask.ingest

import arrow.core.getOrElse
import com.google.common.io.Resources
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.vyne.cask.CaskService
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.websocket.CsvIngestionRequest
import io.vyne.schemaStore.SchemaProvider
import io.vyne.spring.LocalResourceSchemaProvider
import org.apache.commons.csv.CSVFormat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream
import java.nio.file.Paths

class IngesterE2eTest {

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingesterFactory: IngesterFactory
   lateinit var schemaProvider: SchemaProvider
   lateinit var caskService: CaskService
   lateinit var pg: EmbeddedPostgres

   @Before
   fun setup() {
      pg = EmbeddedPostgres.builder().setPort(6660).start()
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:6660/postgres")
         .username("postgres")
         .build()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)

      schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      ingesterFactory = IngesterFactory(jdbcTemplate)
      caskService = CaskService(
         schemaProvider,
         ingesterFactory
      )
   }

   @After
   fun tearDown() {
      pg.close()
   }

   @Test
   fun canIngestCsvToCask() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.csv").toURI()
      val input: Flux<InputStream> = Flux.just(File(source).inputStream())
      val type = caskService.resolveType("OrderWindowSummaryCsv").getOrElse {
         error("Type not found")
      }
      caskService.ingestRequest(
         CsvIngestionRequest(CSVFormat.DEFAULT.withFirstRecordAsHeader(), type, emptySet()),
         input
      ).blockFirst()
   }
}
