package io.vyne.cask.query

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.cask.api.ContentType
import io.vyne.cask.format.csv.CoinbaseOrderSchema
import io.vyne.cask.format.json.CoinbaseJsonOrderSchema
import io.vyne.schemas.fqn
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.apache.commons.io.FileUtils
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.util.*

@Ignore
class CaskDAOIntegrationTest : BaseCaskIntegrationTest() {

   @Rule
   @JvmField
   final val folder = TemporaryFolder()


   @Test
   fun canQueryIngestedDataFromDatabase() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD.json").toURI()

      ingestJsonData(resource, versionedType, taxiSchema)

      caskDao.findBy(versionedType, "symbol", "BTCUSD").size.should.equal(10061)
      caskDao.findBy(versionedType, "open", "6300").size.should.equal(7)
      caskDao.findBy(versionedType, "close", "6330").size.should.equal(9689)
      caskDao.findBy(versionedType, "orderDate", "2020-03-19").size.should.equal(10061)

      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun canCreateCaskRecordTable() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      caskDao.dropCaskRecordTable(versionedType)

      // act
      val tableName = caskDao.createCaskRecordTable(versionedType)

      // assert we can insert to the new cask table
      jdbcTemplate.execute("""insert into $tableName(symbol, open, close, "orderDate") values ('BTCUSD', '6300', '6330', '2020-03-19')""")
   }



   @Test
   fun canAddCaskMessage() {
      // prepare
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val messageId = UUID.randomUUID().toString()

      // act
      caskDao.createCaskMessage(versionedType, messageId, Flux.just(ByteArrayInputStream("Data to ingest".toByteArray(Charsets.UTF_8))), ContentType.json, emptyMap<String,Any>())

      // assert
      val caskMessages = caskMessageRepository.findAll()
      caskMessages.size.should.be.equal(1)
      caskMessages[0].id.should.not.be.empty
      caskMessages[0].messageContentId.should.above(0)
      caskMessages[0].qualifiedTypeName.should.equal(versionedType.fullyQualifiedName)
      caskMessages[0].insertedAt.should.be.below(Instant.now())
   }

   @Test
   fun `can ingest message against two versions of schema and query back`() {
      val taxiSchema = CoinbaseJsonOrderSchema.schemaV1
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = Resources.getResource("Coinbase_BTCUSD_single_v1.json").toURI()

      ingestJsonData(resource, versionedType, taxiSchema)

      val v2Schema = CoinbaseJsonOrderSchema.schemaV2
      val v2Type = v2Schema.versionedType("OrderWindowSummary".fqn())
      val v2Resource = Resources.getResource("Coinbase_BTCUSD_single_v2.json").toURI()

      ingestJsonData(v2Resource, v2Type, v2Schema)

      // Let's query by a 3rd type with no data, just to be sure
      val v3Type = CoinbaseJsonOrderSchema.schemaV3.versionedType("OrderWindowSummary".fqn())
      val records = caskDao.findAll(v3Type)
      records.should.have.size(2)

      FileUtils.cleanDirectory(folder.root)
   }

   @Test
   fun `can ingest large data`() {
      createLargeFile()

      val taxiSchema = CoinbaseOrderSchema.schemaV3
      val versionedType = taxiSchema.versionedType("OrderWindowSummary".fqn())
      val resource = File(folder.root, "largeGuts.csv").toURI()

      ingestCsvData(resource, versionedType, taxiSchema)

      val records = caskDao.findAll(versionedType)
      records.should.have.size(40)

//      FileUtils.cleanDirectory(folder.root)
   }

   private fun createLargeFile() {
      val file = File(folder.root, "largeGuts.csv")
      file.bufferedWriter().use {
         it.write("Date,Symbol,Open,High,Low,Close,Volume BTC,Volume USD${System.lineSeparator()}")
         for (i in 1..40) {
            it.write("2020-03-19 11:12:13,BTCUSD,6300,6330,6186.08,6235.2,817.78,5115937.58${System.lineSeparator()}")
         }
         it.close()
      }
   }

}
