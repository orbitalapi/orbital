package io.vyne.cask.ingest

import com.nhaarman.mockito_kotlin.mock
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariDataSource
import io.vyne.cask.MessageIds
import io.vyne.cask.ddl.TableMetadata
import io.vyne.cask.ddl.TypeDbWrapper
import io.vyne.cask.format.csv.CsvStreamSource
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.cask.query.BaseCaskIntegrationTest
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
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
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
import javax.sql.DataSource

class DataIngestionTests : BaseCaskIntegrationTest() {

   lateinit var ingester: Ingester

   @Test
   fun canIngestWithTimeType() {
      val source = """Entity,Time
         |1,11:11:11
         |2,23:11:44""".trimMargin()
      val schema = TestSchema.schemaTimeTest
      val timeType = schema.versionedType("TimeTest".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, timeType, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(timeType, TypeDbWrapper(timeType, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.timeTypeTest), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(timeType)
      caskDao.createCaskRecordTable(timeType)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["entry"].should.equal("1")
      (result.first()["time"] as Time).toString().should.equal("11:11:11")

      caskDao.createCaskRecordTable(timeType)

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
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
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
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
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
      val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(type, TypeDbWrapper(type, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
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

   }

   @Test
   fun upsertBenchmark() {
      val source = """Id,Name,t1,v1
         |1,Joe,1900-01-01 11:12:13,3.14""".trimMargin()
      val schema = TestSchema.schemaUpsertTest
      val type = schema.versionedType("UpsertTestSinglePk".fqn())

      Benchmark.benchmark("UPSERT to db") { stopwatch ->
         val input: Flux<InputStream> = Flux.just(source.byteInputStream())
         val pipelineSource = CsvStreamSource(input, type, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
         val pipeline = IngestionStream(type, TypeDbWrapper(type, schema), pipelineSource)

         caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.upsertTest), dataSource, caskMessageRepository, configRepository)
         ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
         caskDao.dropCaskRecordTable(type)
         caskDao.createCaskRecordTable(type)
         ingester.ingest().collectList().block()

         stopwatch.stop()

         caskDao.createCaskRecordTable(type)

      }
   }

   @Test
   fun `Can downcast incoming Instant as Date or Time`() {
      //dateOnly: Date(@format = "yyyy-MM-dd'T'HH:mm:ss") by column(1)

      val source = """DateOnly,TimeOnly
         |2013-06-30T00:00:00,2013-05-30T00:00:00
         |2013-06-30T00:00:00,2013-05-30T00:00:00""".trimMargin()
      val schema = TestSchema.schemaTemporalDownCastTest
      val downCastTestType = schema.versionedType("DowncastTest".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, downCastTestType, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(downCastTestType, TypeDbWrapper(downCastTestType, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.temporalSchemaSource), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(downCastTestType)
      caskDao.createCaskRecordTable(downCastTestType)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["dateOnly"].should.equal(Date.valueOf("2013-06-30"))
      (result.first()["timeOnly"] as Time).should.equal(Time.valueOf("00:00:00"))

      caskDao.createCaskRecordTable(downCastTestType)


      //
      val dateOnlyQualifiedName = schema.type("DowncastTest").attributes.getValue("dateOnly").type
      val rawValue = RawObjectMapper.map(
         TypedInstance.from(schema.type(dateOnlyQualifiedName), LocalDate.of(2020, 8, 5), schema, false, setOf(), DefinedInSchema)
      )
      rawValue.should.be.equal("2020-08-05T00:00:00")

      val timeValue = LocalTime.parse("21:06:07")
      val timeOnlyQualifiedName = schema.type("DowncastTest").attributes.getValue("timeOnly").type
      val timeRawValue = try {
         RawObjectMapper.map(
            TypedInstance.from(schema.type(timeOnlyQualifiedName), timeValue, schema, false, setOf(), DefinedInSchema)
         )
      } catch (e: Exception) {
         LocalTime.MAX
      }

      timeRawValue.should.equal("21:06:07")
   }

   @Test
   fun `Can Ingest With Default Values`() {
      val source = """FIRST_COLUMN
         |First
         |Second""".trimMargin()
      val schema = TestSchema.schemaWithDefault
      val modelWithDefaults = schema.versionedType("ModelWithDefaults".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, modelWithDefaults, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(modelWithDefaults, TypeDbWrapper(modelWithDefaults, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.schemaWithDefaultValueSource), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(modelWithDefaults)
      caskDao.createCaskRecordTable(modelWithDefaults)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["field1"].should.equal("First")
      result.first()["defaultString"].should.equal("Default String")
      val defaultDecimal = result.first()["defaultDecimal"] as BigDecimal
      defaultDecimal.compareTo(BigDecimal("1000000.0")).should.equal(0)

      caskDao.createCaskRecordTable(modelWithDefaults)

   }

   @Test
   fun `Can Ingest BY Concatenating Values`() {
      val source = """FIRST_COLUMN,SECOND_COLUMN,THIRD_COLUMN
         |First1,Second1,Third1
         |First2,Second2,Third2""".trimMargin()
      val schema = TestSchema.schemaConcat
      val concatModel = schema.versionedType("ConcatModel".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, concatModel, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(concatModel, TypeDbWrapper(concatModel, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.schemaConcatSource), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(concatModel)
      caskDao.createCaskRecordTable(concatModel)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["concatField"].should.equal("First1-Second1-Third1")
      caskDao.createCaskRecordTable(concatModel)
//
   }

   @Test
   fun `Instant Formatting`() {
      val source = """ValidityPeriodDateAndTime
         |2020-07-31T22:59:59.000000Z
         |2020-08-31T22:59:59.000000Z""".trimMargin()
      val schema = TestSchema.instantSchema
      val instantModel = schema.versionedType("InstantModel".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, instantModel, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(instantModel, TypeDbWrapper(instantModel, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.instantFormatSource), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(instantModel)
      caskDao.createCaskRecordTable(instantModel)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["instant"].toString().should.equal("2020-07-31 22:59:59.0")
      caskDao.createCaskRecordTable(instantModel)
   }

   @Test
   fun `Ingestion of scientific numbers`() {
      val source = """Quantity
         |2.50E+07
         |3.0E+07
         |2500.1234""".trimMargin()
      val schema = TestSchema.decimalSchema
      val decimalModel = schema.versionedType("DecimalModel".fqn())
      val input: Flux<InputStream> = Flux.just(source.byteInputStream())
      val pipelineSource = CsvStreamSource(input, decimalModel, schema, MessageIds.uniqueId(), csvFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader(), ingestionErrorProcessor = caskIngestionErrorProcessor)
      val pipeline = IngestionStream(decimalModel, TypeDbWrapper(decimalModel, schema), pipelineSource)

      caskDao = CaskDAO(jdbcTemplate, SimpleTaxiSchemaProvider(TestSchema.decimalSchemaSource), dataSource, caskMessageRepository, configRepository)
      ingester = Ingester(jdbcTemplate, pipeline, caskIngestionErrorProcessor.sink())
      caskDao.dropCaskRecordTable(decimalModel)
      caskDao.createCaskRecordTable(decimalModel)
      ingester.ingest().collectList().block()

      val result = jdbcTemplate.queryForList("SELECT * FROM ${pipeline.dbWrapper.tableName}")!!
      result.first()["qty"].toString().should.equal("25000000.000000000000000")
      result[1]["qty"].toString().should.equal("30000000.000000000000000")
      result[2]["qty"].toString().should.equal("2500.123400000000000")
   }
}
