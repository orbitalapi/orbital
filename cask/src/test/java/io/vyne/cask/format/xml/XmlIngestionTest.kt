package io.vyne.cask.format.xml

import arrow.core.getOrElse
import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.cask.CaskService
import io.vyne.cask.api.ContentType
import io.vyne.cask.api.XmlIngestionParameters
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.ingest.CaskMessage
import io.vyne.cask.ingest.IngesterFactory
import io.vyne.cask.query.CaskDAO
import io.vyne.cask.websocket.XmlWebsocketRequest
import io.vyne.models.TypedValue
import io.vyne.spring.LocalResourceSchemaProvider
import org.junit.Before
import org.junit.Test
import org.postgresql.PGConnection
import org.postgresql.copy.CopyManager
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Flux
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.nio.file.Paths
import java.sql.Connection
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource

class XmlIngestionTest {
   val jdbcTemplate = mock<JdbcTemplate>()
   private val configRepository = mock< CaskConfigRepository>()
   val dataSource = mock< DataSource>()
   private val connection = mock< Connection>()
   private val pgConnection = mock<PGConnection>()
   private val copyManager = mock< CopyManager>()

   @Before
   fun setUp() {
      whenever(jdbcTemplate.dataSource).thenReturn(dataSource)
      whenever(dataSource.connection).thenReturn(connection)
      whenever(connection.unwrap(eq(PGConnection::class.java))).thenReturn(pgConnection)
      whenever(pgConnection.copyAPI).thenReturn(copyManager)
   }

   @Test
   fun `Cask Service can ingest xml content`() {
      val source = Resources.getResource("Coinbase_BTCUSD_single.xml").toURI()
      val input: Flux<InputStream> = Flux.just(File(source).inputStream())
      val schemaProvider = LocalResourceSchemaProvider(Paths.get(Resources.getResource("schemas/coinbase").toURI()))
      val ingesterFactory = IngesterFactory(jdbcTemplate)
      val caskDAO: CaskDAO = mock()
      val caskService = CaskService(
         schemaProvider,
         ingesterFactory,
         configRepository,
         caskDAO
      )
      val type = caskService.resolveType("OrderWindowSummaryXml").getOrElse {
         error("Type not found")
      }
      whenever(caskDAO.createCaskMessage(any(), any(),  any(), eq(ContentType.xml), any())).thenReturn(
         CaskMessage("message-1", "OrderWindowSummaryXml", 1, Instant.now(), ContentType.xml, "")
      )
      whenever(caskDAO.getMessageContent(eq(1L))).thenReturn(input)
      // When
      val instanceAttributeSet  =  caskService.ingestRequest(
         XmlWebsocketRequest(XmlIngestionParameters(), type),
         input
      ).blockFirst()
      // Then
      instanceAttributeSet.attributes.size.should.be.equal(4)
      val orderDate = instanceAttributeSet.attributes["orderDate"] as TypedValue
      orderDate.value.should.equal(LocalDate.of(2020, 3, 19))
      val open = instanceAttributeSet.attributes["open"] as TypedValue
      open.value.should.equal(BigDecimal("6300"))
      val close = instanceAttributeSet.attributes["close"] as TypedValue
      close.value.should.equal(BigDecimal("6235.2"))
   }
}
