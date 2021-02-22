package io.vyne.cask.format.xml

import arrow.core.getOrElse
import com.google.common.io.Resources
import io.vyne.cask.CaskService
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.query.BaseCaskIntegrationTest
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.spring.LocalResourceSchemaProvider
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.nio.file.Paths

class XmlIngestionTest : BaseCaskIntegrationTest() {


   @Test
   fun `Cask Service can ingest xml content`() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.xml").toURI()
      val input: InputStream = File(source).inputStream()
      val schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      val caskService = CaskService(
         schemaProvider,
         ingesterFactory,
         configRepository,
         caskDao,
         ingestionErrorRepository
      )
      val type = caskService.resolveType("OrderWindowSummaryXml").getOrElse {
         error("Type not found")
      }
//      whenever(caskDAO.createCaskMessage(any(), any(),  any(), eq(ContentType.xml), any())).thenReturn(
//         CaskMessage("message-1", "OrderWindowSummaryXml", 1, Instant.now(), ContentType.xml, "")
//      )
//      whenever(caskDAO.getMessageContent(eq(1L))).thenReturn(input)
      // When
      val instanceAttributeSet  =  caskService.ingestRequest(
         XmlWebsocketRequest(XmlIngestionParameters(), type),
         input
      )
      val lookedUp = caskDao.findAll(type)
      // Then
      TODO()
//      instanceAttributeSet.attributes.size.should.be.equal(4)
//      val orderDate = instanceAttributeSet.attributes["orderDate"] as TypedValue
//      orderDate.value.should.equal(LocalDate.of(2020, 3, 19))
//      val open = instanceAttributeSet.attributes["open"] as TypedValue
//      open.value.should.equal(BigDecimal("6300"))
//      val close = instanceAttributeSet.attributes["close"] as TypedValue
//      close.value.should.equal(BigDecimal("6235.2"))
   }
}
