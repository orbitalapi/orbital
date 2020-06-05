package io.vyne.cask.ingest

import com.fasterxml.jackson.databind.ObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.winterbe.expekt.should
import io.vyne.cask.ddl.PostgresDdlGenerator
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.json.JsonStreamSource
import io.vyne.cask.query.CaskDAO
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.utils.log
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.sql.DataSource


class DbColumnTypesIntegrationTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var pg: EmbeddedPostgres
   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester
   lateinit var dataSource: DataSource
   lateinit var caskDao: CaskDAO

   @Before
   fun setup() {
      pg = EmbeddedPostgres.builder().setPort(6660).start()
      dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:6660/postgres")
         .username("postgres")
         .build()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(schemaStr))
   }

   @After
   fun tearDown() {
      ingester.destroy()
      pg.close()
   }

   private val schemaStr = """
type alias Price as Decimal
type alias Symbol as String
type alias OrderDate as String
type CoinbaseOrder {
    symbol : Symbol by xpath("/Symbol")
    price : Price by xpath("/Price")
    orderDate: Date by xpath("/OrderDate")
    timestamp: Instant by xpath("/Timestamp")
    maturityDate: DateTime by xpath("/MaturityDate")
}""".trimIndent()

   private val coinbaseOrder: ByteArrayInputStream = """
{
"Symbol": "BTCUSD",
"Price": "6186.08",
"OrderDate": "2020-03-19",
"Timestamp": "2020-03-19T13:00:01.000Z",
"MaturityDate": "2011-12-03T10:15:30"
}""".byteInputStream()

   private val taxiSchema = TaxiSchema.from(schemaStr, "Coinbase", "1.0.0")

   @Test
   @Ignore("LENS-136")
   fun testDatabaseColumnTypes() {
      val versionedType = taxiSchema.versionedType("CoinbaseOrder".fqn())
      val input: Flux<InputStream> = Flux.just(coinbaseOrder)

      val pipelineSource = JsonStreamSource(
         input,
         versionedType,
         taxiSchema,
         folder.root.toPath(),
         ObjectMapper())

      val pipeline = IngestionStream(
         versionedType,
         TypeDbWrapper(versionedType, taxiSchema, pipelineSource.cachePath, null),
         pipelineSource)

      ingester = Ingester(jdbcTemplate, pipeline)
      ingester.destroy()
      ingester.initialize()

      ingester
         .ingest()
         .collectList()
         .doOnError { log().error("Ingestion error ", it) }
         .block(Duration.ofMillis(500))

      val tableName = PostgresDdlGenerator.tableName(versionedType)
      val list: MutableList<CoinbaseOrder> = jdbcTemplate.query("Select * from ${tableName}")
      { rs, _ ->
         CoinbaseOrder(
            rs.getString("Symbol"),
            rs.getDouble("Price"),
            rs.getDate("OrderDate").toLocalDate(),
            rs.getTimestamp("Timestamp").toLocalDateTime().atOffset(ZoneOffset.UTC).toInstant(),
            rs.getTimestamp("MaturityDate").toLocalDateTime()
         )
      }

      val anOrder = CoinbaseOrder(
         "BTCUSD",
         6186.08,
         LocalDate.parse("2020-03-19"),
         Instant.parse("2020-03-19T13:00:01.000Z"),
         LocalDateTime.parse("2011-12-03T10:15:30"))
      list.should.contain(anOrder)

      caskDao.findBy(versionedType, "symbol", "BTCUSD").size.should.equal(1)
      caskDao.findBy(versionedType, "orderDate", "2020-03-19").size.should.equal(1)
      caskDao.findBy(versionedType, "timestamp", "2020-03-19T13:00:01.000Z").size.should.equal(1)
   }

   data class CoinbaseOrder(
      val symbol: String,
      val price: Double,
      val orderDate: LocalDate,
      val timestamp: Instant,
      val maturityDate: LocalDateTime)
}

