package io.vyne.cask.query

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal

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
   private val tableName = "OrderWindowSummary_f1b588"
   val typeSchema = lang.taxi.Compiler(schema).compile()
   val taxiSchema = TaxiSchema(typeSchema, listOf())
   val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummary")
   val versionedType = taxiSchema.versionedType(versionedTypeReference)
   lateinit var caskDAO: CaskDAO


   @Before
   fun setUp() {
      caskDAO = CaskDAO(mockJdbcTemplate, SimpleTaxiSchemaProvider(schema))
   }

   @Test
   fun `A decimal alias type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "open", "6300")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("""SELECT * FROM $tableName WHERE "open" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM $tableName WHERE "symbol" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM $tableName WHERE "isRolled" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM $tableName WHERE "high" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM $tableName WHERE "close" = ?""")
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
      statementCaptor.firstValue.should.equal("SELECT * FROM $tableName WHERE \"orderDate\" = ?")
      argsCaptor.firstValue.should.equal("2020-01-01".toLocalDate())
   }

   @Test
   fun `An Instant type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "timestamp", "2020-01-01T12:00:01.000Z")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("SELECT * FROM $tableName WHERE \"timestamp\" = ?")
      argsCaptor.firstValue.should.equal("2020-01-01T12:00:01.000Z".toLocalDateTime())
   }
}
