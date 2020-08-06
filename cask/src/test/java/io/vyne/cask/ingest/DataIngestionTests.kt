package io.vyne.cask.ingest

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.query.CaskDAO
import io.vyne.models.DefinedInSchema
import io.vyne.models.RawObjectMapper
import io.vyne.models.TypedInstance
import io.vyne.schemas.fqn
import io.vyne.spring.SimpleTaxiSchemaProvider
import io.vyne.utils.Benchmark
import org.apache.commons.csv.CSVFormat
import org.apache.commons.io.FileUtils
import org.flywaydb.core.Flyway
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalTime

class DataIngestionTests {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(0) }

   lateinit var jdbcTemplate: JdbcTemplate
   lateinit var ingester: Ingester
   lateinit var caskDao: CaskDAO

   @Before
   fun setup() {
      val dataSource = DataSourceBuilder.create()
         .url("jdbc:postgresql://localhost:${pg.embeddedPostgres.port}/postgres")
         .username("postgres")
         .build()
      Flyway.configure()
         .dataSource(dataSource)
         .load()
         .migrate()
      jdbcTemplate = JdbcTemplate(dataSource)
      jdbcTemplate.execute(TableMetadata.DROP_TABLE)
      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(CoinbaseJsonOrderSchema.sourceV1))
   }

   @Test
   fun canIngestWithTimeType() {
      val source = """Entity,Time
         |1,11:11:11
         |2,23:11:44""".trimMargin()
      val schema = TestSchema.schemaTimeTest
      val timeType = schema.versionedType("TimeTest".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, timeType, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(timeType, TypeDbWrapper(timeType, schema, pipelineSource.cachePath, null), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.timeTypeTest))
      ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(timeType)
      caskDao.createCaskRecordTable(timeType)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["entry"].should.equal("1")
      (result.first()["time"] as Time).toString().should.equal("11:11:11")

      caskDao.createCaskRecordTable(timeType)
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canUpsertWithNoPk() {
      val source = """Id,Name,t1,v1
         |1,Joe,1900-01-01 11:12:13,3.14
         |2,Herb,2010-02-03 21:22:23,1.8
         |1,Django,2000-01-01 01:02:03,6.6""".trimMargin()
      val schema = TestSchema.schemaUpsertTest
      val type = schema.versionedType("UpsertTestNoPk".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema, pipelineSource.cachePath, null), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest))
      ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(type)
      caskDao.createCaskRecordTable(type)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.size.should.equal(3)
      result.first()["id"].should.equal(1)
      result.first()["name"].toString().should.equal("Joe")
      (result.first()["t1"] as Timestamp).toString().should.equal("1900-01-01 11:12:13.0")
      (result.first()["v1"] as BigDecimal).setScale(2, RoundingMode.DOWN).should.equal(BigDecimal(3.14).setScale(2, RoundingMode.DOWN))

      caskDao.createCaskRecordTable(type)
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canUpsertWithSinglePk() {
      val source = """Id,Name,t1,v1
         |1,Joe,1900-01-01 11:12:13,3.14
         |2,Herb,2010-02-03 21:22:23,1.8
         |1,Django,2000-01-01 01:02:03,6.6""".trimMargin()
      val schema = TestSchema.schemaUpsertTest
      val type = schema.versionedType("UpsertTestSinglePk".fqn())

      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema, pipelineSource.cachePath, null), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest))
      ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(type)
      caskDao.createCaskRecordTable(type)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.size.should.equal(2)
      result[1]["id"].should.equal(1)
      result[1]["name"].toString().should.equal("Django")
      (result[1]["t1"] as Timestamp).toString().should.equal("2000-01-01 01:02:03.0")
      (result[1]["v1"] as BigDecimal).setScale(1).should.equal(BigDecimal("6.6"))

      caskDao.createCaskRecordTable(type)
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canUpsertWithMultiPk() {
      val source = """Id,Name,t1,v1
         |1,Joe,1900-01-01 11:12:13,3.14
         |2,Herb,2010-02-03 21:22:23,1.8
         |1,Joe,2000-01-01 01:02:03,6.6""".trimMargin()
      val schema = TestSchema.schemaUpsertTest
      val type = schema.versionedType("UpsertTestMultiPk".fqn())

      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, type, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema, pipelineSource.cachePath, null), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest))
      ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(type)
      caskDao.createCaskRecordTable(type)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.size.should.equal(2)
      result[1]["id"].should.equal(1)
      result[1]["name"].toString().should.equal("Joe")
      (result[1]["t1"] as Timestamp).toString().should.equal("2000-01-01 01:02:03.0")
      (result[1]["v1"] as BigDecimal).setScale(1).should.equal(BigDecimal("6.6"))

      caskDao.createCaskRecordTable(type)
      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun upsertBenchmark() {
      val source = """Id,Name,t1,v1
         |1,Joe,1900-01-01 11:12:13,3.14""".trimMargin()
      val schema = TestSchema.schemaUpsertTest
      val type = schema.versionedType("UpsertTestSinglePk".fqn())

      Benchmark.benchmark("UPSERT to db") { stopwatch ->
         val input: Flux<InputStream> = Flux.just(source.byteInputStream())
         val pipelineSource = CsvStreamSource(input, type, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
         val pipeline = IngestionStream(type, TypeDbWrapper(type, schema, pipelineSource.cachePath, null), pipelineSource)

         caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest))
         ingester = Ingester(jdbcTemplate, pipeline)
         caskDao.dropCaskRecordTable(type)
         caskDao.createCaskRecordTable(type)
         ingester.ingest().collectList().block()

         stopwatch.stop()

         caskDao.createCaskRecordTable(type)
         FileUtils.cleanDirectory(folder.root)
      }
   }

   @Test
   fun `Can downcast incoming Instant as Date or Time`() {
      val source = """DateOnly,TimeOnly
         |2013-06-30T00:00:00,2013-05-30T00:00:00
         |2013-06-30T00:00:00,2013-05-30T00:00:00""".trimMargin()
      val schema = TestSchema.schemaTemporalDownCastTest
      val downCastTestType = schema.versionedType("DowncastTest".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, downCastTestType, schema, folder.root.toPath(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader())
      val pipeline = IngestionStream(downCastTestType, TypeDbWrapper(downCastTestType, schema, pipelineSource.cachePath, null), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.temporalSchemaSource))
      ingester = Ingester(jdbcTemplate, pipeline)
      caskDao.dropCaskRecordTable(downCastTestType)
      caskDao.createCaskRecordTable(downCastTestType)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["dateOnly"].should.equal(Date.valueOf("2013-06-30"))
      (result.first()["timeOnly"] as Time).should.equal(Time.valueOf("00:00:00"))

      caskDao.createCaskRecordTable(downCastTestType)
      FileUtils.cleanDirectory(folder.root)

      //
      val dateOnlyQualifiedName = schema.type("DowncastTest").attributes.getValue("dateOnly").type
      val rawValue = RawObjectMapper.map(
         TypedInstance.from(schema.type(dateOnlyQualifiedName), LocalDate.of(2020, 8, 5), schema, false, setOf(), DefinedInSchema)
      )
      rawValue.should.be.equal("2020-08-06T00:00:00")

      val timeOnlyQualifiedName = schema.type("DowncastTest").attributes.getValue("timeOnly").type
      val timeRawValue = try { RawObjectMapper.map(
         TypedInstance.from(schema.type(timeOnlyQualifiedName), LocalTime.now(), schema, false, setOf(), DefinedInSchema)
      ) } catch(e: Exception) {
        LocalTime.MAX
      }

      timeRawValue.should.be.equal(LocalTime.MAX)
   }
}
