package io.vyne.connectors

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.connectors.calcite.VyneCalciteDataSource
import io.vyne.models.TypedInstance
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test
import java.math.BigDecimal

class CsvQueryTest {
   private val testType = "OrderWindowSummary"
   private val defaultSchema = """
         type alias Price as Decimal
         type alias Symbol as String
          @io.vyne.formats.Csv(
                     delimiter = ",",
                     nullValue = "NULL"
                  )
         type $testType {
             symbol : Symbol by column(2)
             open : Price by column(3)
             // Added column
             high : Price by column(4)
             // Changed column
             close : Price by column(6)
         }
""".trimIndent()
   private val schema = TaxiSchema.fromStrings(
      listOf(defaultSchema)
   )

   @Test
   fun `can query a csv file`() {
      val orderWindowType = schema.type(testType)
      val typedInstanceStream =  Resources.getResource("Coinbase_BTCUSD_3rows.csv").openStream().bufferedReader().lines().map {
         TypedInstance.from(
            orderWindowType,
            it,
            schema
         )
      }

      VyneCalciteDataSource(schema, QualifiedName(testType), typedInstanceStream).connection.use { connection ->
         val statement = connection.createStatement()
         val resultSet = statement.executeQuery("""select * from $testType t0 where t0."close" = 6235.2""")
         resultSet.next().should.be.`true`
         resultSet.getString(1).should.equal("BTCUSD")
         resultSet.getObject(4).should.equal(BigDecimal("6235.2"))
         resultSet.next().should.be.`false`
      }
   }
}
