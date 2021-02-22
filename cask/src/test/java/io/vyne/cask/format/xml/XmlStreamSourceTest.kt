package io.vyne.cask.format.xml

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.cask.MessageIds
import io.vyne.models.TypedNull
import io.vyne.models.TypedValue
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.Benchmark
import io.vyne.utils.Benchmark.benchmark
import io.vyne.utils.log
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

class XmlStreamSourceTest {
   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `Can ingest list of orders provided in a single xml document`() {
      val schema = XmlTestSchema.schemaV1
      val typeReference = "OrderWindowSummaryXml"
      val versionedType = schema.versionedType(VersionedTypeReference.parse(typeReference))
      val resource = Resources.getResource("Coinbase_BTCUSD.xml").toURI()

      // Ingest it a few times to get an average performance
      Benchmark.benchmark("Can ingest list of orders provided in a single xml document", 10 ,10) {
         val stream = XmlStreamSource(
            File(resource).inputStream(),
            versionedType,
            schema,
            MessageIds.uniqueId(),
            "root/Order")
         val noOfMappedRows = stream
            .sequence().count()

         noOfMappedRows.should.equal(10061)
         log().info("Mapped ${noOfMappedRows} rows to typed instance")
      }
   }

   @Test
   fun `Can ingest a single order from xml`() {
      val schema = XmlTestSchema.schemaV1
      val typeReference = "OrderWindowSummaryXml"
      val versionedType = schema.versionedType(VersionedTypeReference.parse(typeReference))
      val resource = Resources.getResource("Coinbase_BTCUSD_single.xml").toURI()
      val stream = XmlStreamSource(
         File(resource).inputStream(),
         versionedType,
         schema,
         MessageIds.uniqueId()
      )
      val instanceAttributeSet = stream
         .sequence()
         .first()
      instanceAttributeSet.attributes.size.should.equal(5)
      val orderDate = instanceAttributeSet.attributes["orderDate"] as TypedValue
      orderDate.value.should.equal(LocalDate.of(2020, 3, 19))
      val open = instanceAttributeSet.attributes["open"] as TypedValue
      open.value.should.equal(BigDecimal("6300"))
      val close = instanceAttributeSet.attributes["close"] as TypedValue
      close.value.should.equal(BigDecimal("6235.2"))
      val volume = instanceAttributeSet.attributes["volume"] as TypedNull?
      volume.should.not.`null`
   }

   @Test
   fun canIngestFpmlDocument() {
      val schema = TaxiSchema.from(
         Resources.getResource("fpml/efira-swap.taxi").readText()
      )
      val versionedType = schema.versionedType(VersionedTypeReference.parse("Swap"))
      val resource = Resources.getResource("fpml/EFIRA_SWAP_OTR_4461642_20161220112630149.xml").toURI()
      benchmark("Ingest complex XML document", warmup = 50, iterations = 500) {
         val stream = XmlStreamSource(
            File(resource).inputStream(),
            versionedType,
            schema,
            MessageIds.uniqueId()
         )
         stream.sequence().first()
      }
   }
}
