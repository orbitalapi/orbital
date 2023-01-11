package io.vyne.cask.ddl.views

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.asPackage
import io.vyne.cask.api.CaskConfig
import io.vyne.cask.config.CaskConfigRepository
import io.vyne.cask.config.StringToQualifiedNameConverter
import io.vyne.cask.ddl.views.ViewJoin.ViewJoinKind.LEFT_OUTER
import io.vyne.from
import io.vyne.schema.api.SchemaSet
import io.vyne.schemaStore.SimpleSchemaStore
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.toParsedPackages
import lang.taxi.types.ObjectType
import lang.taxi.types.QualifiedName
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.Assertions

@Ignore("Views not currently supported")
class CaskViewBuilderFactoryTest {

   lateinit var builderBuilderFactory: CaskViewBuilderFactory
   lateinit var repository: CaskConfigRepository

   val versionedSource = VersionedSource.sourceOnly("""
         namespace test
         type TransactionEvent
         type NotionalQuantityRequired inherits Int
         type QuantityRequired inherits Int
         type UnitMultiplier inherits Int
         type TradeDate inherits Date
         type TradeTime inherits Time
         type TradeTimestamp inherits Instant
         type Order {
            @Id
            id : OrderId as String
            @Format( "YYYYDDTHH:nn:ss" )
            lastTradeDate : TradeDate
            @Format( "HH:nn:ss" )
            lastTradeTime : TradeTime
            timestampt : TradeTimestamp by ( this.lastTradeDate + this.lastTradeTime )
            tradeStatus : TradeStatus as String
            notionalRequired : NotionalQuantityRequired by (QuantityRequired * UnitMultiplier)
         }
         type Trade {
            id : TradeId as String
            orderId : OrderId
            @Between
            tradeDate : TradeDate
         }
      """)
   val taxiSchema = TaxiSchema.from(versionedSource)

   val viewDef = CaskViewDefinition(
      QualifiedName.from("test.OrderEvent"),
      distinct = true,
      inherits = listOf(QualifiedName.from("test.TransactionEvent")),
      join = ViewJoin(
         kind = LEFT_OUTER,
         left = QualifiedName.from("test.Order"),
         right = QualifiedName.from("test.Trade"),
         joinOn = listOf(
            JoinExpression(
               leftField = "id",
               rightField = "orderId"
            ),
            JoinExpression(
               leftField = "lastTradeDate",
               rightField = "tradeDate"
            )
         )
      ),
      whereClause = "test.Order:tradeStatus = 'Cf'"
   )


   @Before
   fun setup() {
      repository = mock { }
      val schemaStore = SimpleSchemaStore()
      schemaStore.setSchemaSet(SchemaSet.fromParsed(listOf(versionedSource).asPackage().toParsedPackages(), 1))
      builderBuilderFactory = CaskViewBuilderFactory(repository, schemaStore, StringToQualifiedNameConverter())
      whenever(repository.findAllByQualifiedTypeName(eq("test.Order"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("test.Order".fqn()), "orders", daysToRetain = 100000)
         ))
      whenever(repository.findAllByQualifiedTypeName(eq("test.Trade"))).thenReturn(
         listOf(CaskConfig.forType(taxiSchema.versionedType("test.Trade".fqn()), "trades", daysToRetain = 100000)
         ))

   }

   @Test
   fun `generates taxi type for view`() {
      val generatedTaxi = builderBuilderFactory.getBuilder(viewDef).generateTaxiSource()
      val expected = """namespace test {
   [[ Generated by Cask.  Source types are test.Order,test.Trade ]]
   @Generated
   model OrderEvent inherits TransactionEvent {
      @Id order_Id : OrderId
      @Format( "YYYYDDTHH:nn:ss" )
      order_LastTradeDate : TradeDate
      @Format( "HH:nn:ss" )
      order_LastTradeTime : TradeTime
      order_Timestampt : TradeTimestamp  by this.order_LastTradeDate + this.order_LastTradeTime
      order_TradeStatus : TradeStatus
      trade_Id : TradeId
      @Between trade_TradeDate : TradeDate
   }
}"""
      Assertions.assertEquals(expected.trimNewLines(), generatedTaxi.trimNewLines())
      generatedTaxi.trimNewLines().should.equal(expected.trimNewLines())
   }

   @Test
   fun `generates create view ddl`() {
      val statements = builderBuilderFactory.getBuilder(viewDef).generateCreateView()
      val expectedDrop = """drop view if exists v_OrderEvent;"""
      val expectedCreate = """create or replace view v_OrderEvent as
select distinct
"orders"."id" as "order_Id",
"orders"."lastTradeDate" as "order_LastTradeDate",
"orders"."lastTradeTime" as "order_LastTradeTime",
"orders"."timestampt" as "order_Timestampt",
"orders"."tradeStatus" as "order_TradeStatus",
"orders".caskmessageid as caskmessageid,
"trades"."id" as "trade_Id",
"trades"."tradeDate" as "trade_TradeDate"
from
"orders"
left outer join "trades" on "orders"."id" = "trades"."orderId" and "orders"."lastTradeDate" = "trades"."tradeDate"
WHERE "orders"."tradeStatus" = 'Cf';"""
      val (actualDrop, actualCreate) = statements
      actualDrop.trimNewLines().should.equal(expectedDrop.trimNewLines())
      actualCreate.trimNewLines().should.equal(expectedCreate.trimNewLines())
   }

   @Test
   fun `when converting where clauses, type field names are converted to correct column names`() {
      val types = listOf(
         taxiSchema.type("test.Order").taxiType as ObjectType,
         taxiSchema.type("test.Trade").taxiType as ObjectType
      )
      val tableNames = mapOf(
         QualifiedName.from("test.Order") to "Orders",
         QualifiedName.from("test.Trade") to "Trades"
      )

      // Date in test to explore edge cases of regex
      val builder = builderBuilderFactory.getBuilder(viewDef)
      builder.convertWhereClause("test.Order:tradeStatus = 'Cf' and test.Trade:tradeDate >= '2020-03-05T22:30:00'", types, tableNames).let { converted ->
         val expected = """ "Orders"."tradeStatus" = 'Cf' and "Trades"."tradeDate" >= '2020-03-05T22:30:00' """.trim()
         converted.should.equal(expected)
      }

      builder.convertWhereClause("""
         test.Order:tradeStatus = 'Cf'
         and CASE
          when (test.Order:tradeStatus = 'Cf' and test.Trade:id is not null) then 1
          else 0
          end) = 0
      """.trimIndent(), types, tableNames).let { converted ->
         val expected = """ "Orders"."tradeStatus" = 'Cf'
and CASE
 when ("Orders"."tradeStatus" = 'Cf' and "Trades"."id" is not null) then 1
 else 0
 end) = 0 """.trim()
         converted.should.equal(expected)
      }
   }

}

fun String.trimNewLines(): String {
   return this
      .lines()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .joinToString("")
}
