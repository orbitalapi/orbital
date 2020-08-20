package io.vyne.cask.query

import com.nhaarman.mockitokotlin2.*
import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariDataSource
import io.vyne.VersionedTypeReference
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.spring.SimpleTaxiSchemaProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
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
   val typeSchema = lang.taxi.Compiler(schema).compile()
   val taxiSchema = TaxiSchema(typeSchema, listOf())
   val versionedTypeReference = VersionedTypeReference.parse("OrderWindowSummary")
   val versionedType = taxiSchema.versionedType(versionedTypeReference)
   lateinit var caskDAO: CaskDAO
   lateinit var largeObjectDataSourceProps: DataSourceProperties

   @Rule
   @JvmField
   val pg = EmbeddedPostgresRules.singleInstance().customize { it.setPort(0) }

   @Before
   fun setUp() {
      largeObjectDataSourceProps = DataSourceProperties()
      largeObjectDataSourceProps.driverClassName = "org.postgresql.Driver"
      largeObjectDataSourceProps.url = "jdbc:postgresql://localhost:${pg.embeddedPostgres.port}/postgres"
      largeObjectDataSourceProps.username = "postgres"

      caskDAO = CaskDAO(mockJdbcTemplate, SimpleTaxiSchemaProvider(schema), largeObjectDataSourceProps)
      whenever(mockJdbcTemplate.queryForList(
         eq("SELECT tablename from cask_config where qualifiedtypename = ?"),
         eq(listOf(versionedType.fullyQualifiedName).toTypedArray()),
         eq(String::class.java))).thenReturn(listOf("rderWindowSummary_f1b588_de3f20"))
   }

   @Test
   fun `A decimal alias type can be queried correctly via findBy`() {
      // when
      caskDAO.findBy(versionedType, "open", "6300")
      // then
      val statementCaptor = argumentCaptor<String>()
      val argsCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(1)).queryForList(statementCaptor.capture(), argsCaptor.capture())
      statementCaptor.firstValue.should.equal("""SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE "open" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE "symbol" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE "isRolled" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE "high" = ?""")
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
      statementCaptor.firstValue.should.equal("""SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE "close" = ?""")
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
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE \"orderDate\" = ?")
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
      statementCaptor.firstValue.should.equal("SELECT * FROM rderWindowSummary_f1b588_de3f20 WHERE \"timestamp\" = ?")
      argsCaptor.firstValue.should.equal("2020-01-01T12:00:01.000Z".toLocalDateTime())
   }

   @Test
   fun `select clauses generate outer joins for all tables`() {
      CaskDAO.selectTableList(listOf("table1")).should.equal("table1 t0")
      CaskDAO.selectTableList(listOf("table1", "table2")).should.equal("table1 t0 full outer join table2 t1 on 0 = 1")
      CaskDAO.selectTableList(listOf("table1", "table2", "table3")).should.equal("table1 t0 full outer join table2 t1 on 0 = 1 full outer join table3 t2 on 0 = 1")
   }

   @Test
   fun `find Between should query all relevant tables for given type`() {
      // given
      val start = "2020-01-01T12:00:01.000Z"
      val end = "2020-10-01T12:00:01.000Z"
      whenever(mockJdbcTemplate.queryForList(
         eq("SELECT tablename from cask_config where qualifiedtypename = ?"),
         eq(listOf(versionedType.fullyQualifiedName).toTypedArray()),
         eq(String::class.java))).thenReturn(listOf("rderWindowSummary_f1b588_de3f20", "rderWindowSummary_f1b588_ab1g30"))
      caskDAO.findBetween(versionedType, "timestamp", start, end)
      val statementCaptor = argumentCaptor<String>()
      val startDateCaptor = argumentCaptor<Any>()
      val endDateCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(2)).queryForList(statementCaptor.capture(), startDateCaptor.capture(), endDateCaptor.capture())
   }

   @Test
   fun `find After should query all relevant tables for given type`() {
      // given
      val date = "2020-01-01T12:00:01.000Z"
      whenever(mockJdbcTemplate.queryForList(
         eq("SELECT tablename from cask_config where qualifiedtypename = ?"),
         eq(listOf(versionedType.fullyQualifiedName).toTypedArray()),
         eq(String::class.java))).thenReturn(listOf("rderWindowSummary_f1b588_de3f20", "rderWindowSummary_f1b588_ab1g30"))
      caskDAO.findAfter(versionedType, "timestamp", date)
      val statementCaptor = argumentCaptor<String>()
      val argCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(2)).queryForList(statementCaptor.capture(), argCaptor.capture())
   }

   @Test
   fun `find Before should query all relevant tables for given type`() {
      // given
      val date = "2020-01-01T12:00:01.000Z"
      whenever(mockJdbcTemplate.queryForList(
         eq("SELECT tablename from cask_config where qualifiedtypename = ?"),
         eq(listOf(versionedType.fullyQualifiedName).toTypedArray()),
         eq(String::class.java))).thenReturn(listOf("rderWindowSummary_f1b588_de3f20", "rderWindowSummary_f1b588_ab1g30"))
      caskDAO.findBefore(versionedType, "timestamp", date)
      val statementCaptor = argumentCaptor<String>()
      val argCaptor = argumentCaptor<Any>()
      verify(mockJdbcTemplate, times(2)).queryForList(statementCaptor.capture(), argCaptor.capture())
   }
}
