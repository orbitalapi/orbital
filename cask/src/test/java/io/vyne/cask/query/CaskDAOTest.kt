package io.vyne.cask.query

import com.nhaarman.mockitokotlin2.*
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.cask.ddl.TableMetadata
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.RowMapper
import java.math.BigDecimal
import java.time.Instant

class CaskDAOTest {
   private val mockJdbcTemplate = mock<JdbcTemplate>()
   private val schema = """
    type alias Price as Decimal
    type alias Symbol as String
    type OrderWindowSummary {
    symbol : Symbol by xpath("/Symbol")
    open : Price by xpath("/Open")
    // Added column
    high : Double by xpath("/High")
    // Changed column
    close : Int by xpath("/Close")
    isRolled: Boolean
    orderDate: Date
    orderTime: Time
    timestamp: Instant
}

   """.trimIndent()
   val typeSchema = lang.taxi.Compiler(schema).compile()
   val taxiSchema = TaxiSchema(typeSchema, listOf())
   val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummary")
   val versionedType = taxiSchema.versionedType(versionedTypeReference)
   lateinit var caskDAO: CaskDAO


   @Before
   fun setUp() {
      whenever(mockJdbcTemplate.query(any<PreparedStatementCreator>(), any<RowMapper<*>>())).thenReturn(mutableListOf(TableMetadata(
         "rderWindowSummary_f1b588_0641a7",
         "OrderWindowSummary",
         "0641a7",
         listOf("test-schema.taxi:0.1.1"),
         listOf(schema),
         Instant.now(),
         null,
         null
      )))
      caskDAO = CaskDAO(mockJdbcTemplate)
   }

   @Test
   fun `A decimal alias type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "open", "6300")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE open = ?")
      argsCaptor.firstValue.should.equal(BigDecimal("6300"))
   }

   @Test
   fun `A string type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "symbol", "BTCUSD")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE symbol = ?")
      argsCaptor.firstValue.should.equal("BTCUSD")
   }

   @Test
   fun `A bool type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "isRolled", "True")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE isRolled = ?")
      argsCaptor.firstValue.should.equal(true)
   }

   @Test
   fun `A double type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "high", "6300.0")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE high = ?")
      argsCaptor.firstValue.should.equal(BigDecimal("6300.0"))
   }

   @Test
   fun `An Int type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "close", "1")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE close = ?")
      argsCaptor.firstValue.should.equal(BigDecimal("1"))
   }

   @Test
   fun `A Date type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "orderDate", "2020-01-01")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE orderDate = ?")
      argsCaptor.firstValue.should.equal("2020-01-01".toLocalDate())
   }

   @Test
   fun `An Instant type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "timestamp", "2020-01-01T12:00:01.000")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_0641a7 WHERE timestamp = ?")
      argsCaptor.firstValue.should.equal("2020-01-01T12:00:01.000".toLocalDateTime())
   }
}
