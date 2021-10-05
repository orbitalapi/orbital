package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.testVyne
//import io.vyne.testVyne
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import java.time.Instant

class ConditionalFieldReaderTest {
   val schema = """
type alias CurrencySymbol as String
type Money {
   quantity : Decimal
   currency : CurrencySymbol
}
type alias CounterpartyId as String

type DealtAmount inherits Money // hehehehe
type SettlementAmount inherits Money
type TradeRecord {
    // define simple properties by xpath expressions
    counterparty : CounterpartyId by xpath("//counterpartyId")
    ccy1 : CurrencySymbol by xpath("//baseAmount/@ccy")
    ccy2 : CurrencySymbol by xpath("//termAmount/@ccy")
    (   dealtAmount : DealtAmount
        settlementAmount : SettlementAmount
    ) by when( xpath("//rate/@ccy") : CurrencySymbol) {
        ccy1 -> {
            dealtAmount(
                quantity by xpath("//baseAmount")
                currency = ccy1
            )
            settlementAmount(
                quantity by xpath("//termAmount")
                currency = ccy2
            )
        }
        ccy2 -> {
            dealtAmount(
                quantity by xpath("//termAmount")
                currency = ccy2
            )
            settlementAmount(
                quantity by xpath("//baseAmount")
                currency = ccy1
            )
        }
    }
}

type TransformedTradeRecord {
   counterpartyId : CounterpartyId
   dealtAmount : DealtAmount
}
      """.trimIndent()

   @Test
   @Ignore("Destructured objects are not currently supported")
   fun canReadAnObjectWithConditionalFields() {

      val (vyne, _) = testVyne(schema)
      val xml = """
<tradeRecord>
    <counterpartyId>client001</counterpartyId>
    <baseAmount ccy="GBP">1000000</baseAmount>
    <termAmount ccy="USD">800000</termAmount>
    <rate ccy="GBP">0.8000</rate>
</tradeRecord>
      """.trimIndent()
      val tradeRecord = TypedInstance.from(vyne.schema.type("TradeRecord"), xml, vyne.schema, source = Provided) as TypedObject
      tradeRecord.type.fullyQualifiedName.should.equal("TradeRecord")
      tradeRecord["ccy1"].value.should.equal("GBP")
      tradeRecord["ccy2"].value.should.equal("USD")
      val dealtAmount = tradeRecord["dealtAmount"] as TypedObject
      dealtAmount.type.fullyQualifiedName.should.equal("DealtAmount")
      dealtAmount["quantity"].type.fullyQualifiedName.should.equal("lang.taxi.Decimal")
      dealtAmount["quantity"].value.should.equal(1_000_000.toBigDecimal())
      dealtAmount["currency"].type.fullyQualifiedName.should.equal("CurrencySymbol")
      dealtAmount["currency"].value.should.equal("GBP")

      val settlementAmount = tradeRecord["settlementAmount"] as TypedObject
      settlementAmount.type.fullyQualifiedName.should.equal("SettlementAmount")
      settlementAmount["quantity"].type.fullyQualifiedName.should.equal("lang.taxi.Decimal")
      settlementAmount["quantity"].value.should.equal(800_000.toBigDecimal())
      settlementAmount["currency"].type.fullyQualifiedName.should.equal("CurrencySymbol")
      settlementAmount["currency"].value.should.equal("USD")
   }

   @Test
   @Ignore("Destructured objects are not currently supported")
   fun canTransformUsingConditionalDataAsSource() {
      val (vyne, _) = testVyne(schema)
      val xml = """
<tradeRecord>
    <counterpartyId>client001</counterpartyId>
    <baseAmount ccy="GBP">1000000</baseAmount>
    <termAmount ccy="USD">800000</termAmount>
    <rate ccy="GBP">0.8000</rate>
</tradeRecord>
      """.trimIndent()
      val tradeRecord = TypedInstance.from(vyne.schema.type("TradeRecord"), xml, vyne.schema, source = Provided) as TypedObject
      val queryContext = vyne.query()
      queryContext.addFact(tradeRecord)
      val result = runBlocking {queryContext.build("TransformedTradeRecord")}
      result.isFullyResolved.should.be.`true`
   }

   @Test
   fun conditionalFieldSelectedOnConstantLiteral() {

      val (vyne, _) = testVyne( """
      type Direction inherits String
      type BankDirection inherits Direction
      type ClientDirection inherits Direction
      type Order {
         bankDirection: BankDirection
         clientDirection: ClientDirection by when (this.bankDirection) {
            "Buy" -> "Sell"
            "Sell" -> "Buy"
         }
      }
      """)
      val json = """{ "bankDirection" : "Buy" }"""
      val order = TypedObjectFactory(vyne.schema.type("Order"), json, vyne.schema, source = Provided).build() as TypedObject

      order["clientDirection"].value!!.should.equal("Sell")
      order["bankDirection"].value!!.should.equal("Buy")
   }

   @Test
   fun conditionalWithNoMatchSetsNull() {
      val (vyne, _) = testVyne( """
      type Order {
         bankDirection: BankDirection as String
         clientDirection: ClientDirection as String by when (this.bankDirection) {
            "Buy" -> "Sell"
            "Sell" -> "Buy"
            else -> null
         }
      }
      """)
      val json = """{ "bankDirection" : "buy" }"""
      val order = TypedObjectFactory(vyne.schema.type("Order"), json, vyne.schema, source = Provided).build() as TypedObject

      order["bankDirection"].value!!.should.equal("buy")
      order["clientDirection"].value.should.be.`null`

   }

   @Test
   fun `can use a function inside the when selection clause`() {
      val (vyne, _) = testVyne( """
      type Order {
         bankDirection: BankDirection as String
         clientDirection: ClientDirection as String by when (upperCase(this.bankDirection)) {
            "BUY" -> "Sell"
            "SELL" -> "Buy"
            else -> null
         }
      }
      """)
      val json = """{ "bankDirection" : "buy" }"""
      val order = TypedObjectFactory(vyne.schema.type("Order"), json, vyne.schema, source = Provided).build() as TypedObject

      order["bankDirection"].value!!.should.equal("buy")
      order["clientDirection"].value.should.equal("Sell")
   }

   @Ignore("this feature isn't implemented yet")
   @Test
   fun conditionalFieldSelectedOnConstantEnum() {

      val (vyne, _) = testVyne("""
      type Direction inherits String
      type BankDirection inherits Direction
      type ClientDirection inherits Direction
      type Order {
         bankDirection: BankDirection
         clientDirection: ClientDirection by when (BankDirection) {
            "Buy" -> "Sell"
            "Sell" -> "Buy"
         }
      }
      """)
      val order = TypedInstance.from(vyne.schema.type("Order"), """{ bankDirection : "Buy" }""", vyne.schema, source = Provided) as TypedObject
      order["clientDirection"].value!!.should.equal("Sell")
//      val queryContext = vyne.query()
//      vyne.addJsonModel("Order", """{ bankDirection : "Buy" }""")


   }

   @Test
   fun `can evaluate functions in where clauses in xml`() {
      val (vyne, _) = testVyne("""
         model Foo {
            assetClass : String by xpath("/Foo/assetClass")
            identifierValue : String? by when (this.assetClass) {
               "FXD" -> left(xpath("/Foo/symbol"),6)
               else -> xpath("/Foo/isin")
            }
         }""")

      fun xml(assetClass: String) = """<Foo>
         |<assetClass>$assetClass</assetClass>
         |<symbol>GBPUSD-100293</symbol>
         |<isin>ISIN-138443</isin>
         |</Foo>
      """.trimMargin()

      val fooWithSymbol = TypedInstance.from(vyne.type("Foo"), xml("FXD"), vyne.schema, source = Provided) as TypedObject
      fooWithSymbol["identifierValue"].value.should.equal("GBPUSD")

      val fooWithIsin = TypedInstance.from(vyne.type("Foo"), xml("xxx"), vyne.schema, source = Provided) as TypedObject
      fooWithIsin["identifierValue"].value.should.equal("ISIN-138443")
   }

   @Test
   fun `can evaluate functions in where clauses in json`() {
      val (vyne, _) = testVyne("""
         model Foo {
            assetClass : String
            identifierValue : String? by when (this.assetClass) {
               "FXD" -> left(jsonPath("$.symbol"),6)
               else -> jsonPath("$.isin")
            }
         }""")
      fun json(assetClass: String) = """{
      "assetClass" : "$assetClass",
      "symbol" : "GBPUSD-100293",
      "isin" : "ISIN-138443"
      }
      """.trimMargin()

      val fooWithSymbol = TypedInstance.from(vyne.type("Foo"), json("FXD"), vyne.schema, source = Provided) as TypedObject
      fooWithSymbol["identifierValue"].value.should.equal("GBPUSD")

      val fooWithIsin = TypedInstance.from(vyne.type("Foo"), json("xxx"), vyne.schema, source = Provided) as TypedObject
      fooWithIsin["identifierValue"].value.should.equal("ISIN-138443")
   }

   @Test
   fun `can evaluate functions in where clauses in csv`() {
      val (vyne, _) = testVyne("""
         model Foo {
            assetClass : String by column(1)
            identifierValue : String? by when (this.assetClass) {
               "FXD" -> left(column(2),6)
               else -> column(3)
            }
         }""")
      fun csv(assetClass: String) = """$assetClass,GBPUSD-100293,ISIN-138443"""
      val fooWithSymbol = TypedInstance.from(vyne.type("Foo"), csv("FXD"), vyne.schema, source = Provided) as TypedObject
      fooWithSymbol["identifierValue"].value.should.equal("GBPUSD")

      val fooWithIsin = TypedInstance.from(vyne.type("Foo"), csv("xxx"), vyne.schema, source = Provided) as TypedObject
      fooWithIsin["identifierValue"].value.should.equal("ISIN-138443")
   }

   @Test
   fun canDeclareSingleFieldConditionalAssignmentsUsingEnumsInAssignment() {
      val (vyne, _) = testVyne("""
      type Direction inherits String
      enum PayReceive {
         Pay,
         Receive
      }
      type BankDirection inherits Direction
      type Order {
         bankDirection: BankDirection
         payReceive: PayReceive by when (this.bankDirection) {
            "Buy" -> PayReceive.Pay
            "Sell" -> PayReceive.Receive
            else -> null
         }
      }
      """)

      val order = TypedInstance.from(vyne.schema.type("Order"), """{ "bankDirection" : "Buy" }""", vyne.schema, source = Provided) as TypedObject
      order["payReceive"].value.should.equal("Pay")

      val orderWithNull = TypedInstance.from(vyne.schema.type("Order"), """{ "bankDirection" : "xxxx" }""", vyne.schema, source = Provided) as TypedObject
      orderWithNull["payReceive"].value.should.be.`null`
   }

   @Test
   fun `can declare calculated field based on other fields`() {
      val (vyne, _) = testVyne("""

      type FirstName inherits String
      type LastName inherits String
      type FullName inherits String
      type BirthDate inherits Date
      type BirthTime inherits Time
      type BirthDateTime inherits Instant
      type Initial inherits String

      model Person {
                  firstName: FirstName
                  lastName: LastName
                  middleName: String
                  birthDate: BirthDate
                  birthTime: BirthTime
                  birthDateAndTime: BirthDateTime by (this.birthDate + this.birthTime)
                  fullName : FullName by (this.firstName + this.lastName)
                  initial: Initial by left(this.firstName, 1)
                  concatName: String by concat(this.firstName,  '-', this.middleName, '-', this.lastName)
               }
      """)

      val order = TypedInstance.from(
         vyne.schema.type("Person"),
         """
            {
               "firstName" : "John",
               "middleName": "Foo",
                "lastName": "Doe",
                "birthDate": "1970-01-02",
                "birthTime": "12:13:14"
            }
         """.trimIndent(),
         vyne.schema, source = Provided)
         as TypedObject
      order["fullName"].value.should.equal("JohnDoe")
      order["birthDateAndTime"].value.should.equal(Instant.parse("1970-01-02T12:13:14Z"))
      order["initial"].value.should.equal("J")
      order["concatName"].value.should.equal("John-Foo-Doe")
   }

   // This failure looks like a genuine bug.
   @Test
   fun `can handle logical expressions for when cases during read`() {
      val (vyne, _) = testVyne("""
         model ComplexWhen {
            trader: String?
            status: String?
            initialQuantity: Decimal?
            leavesQuantity: Decimal?
            quantityStatus: String by when {
                this.initialQuantity == this.leavesQuantity -> "ZeroFill"
                this.trader == "Marty" || this.status == "Foo" -> "ZeroFill"
                this.leavesQuantity > 0 && this.leavesQuantity < this.initialQuantity -> "PartialFill"
                this.leavesQuantity > 0 && this.leavesQuantity < this.initialQuantity || this.trader == "Jimmy" || this.status == "Pending"  -> "FullyFilled"
                else -> "CompleteFill"
            }
         }
      """.trimIndent())
      fun json(trader: String) = """{
      "trader" : "$trader",
      "status" : "Pending",
      "initialQuantity" : 100,
      "leavesQuantity": 1
      }
      """.trimMargin()

      val typedObject = TypedInstance.from(vyne.type("ComplexWhen"), json("Jimmy"), vyne.schema, source = Provided) as TypedObject
      typedObject["quantityStatus"].value.should.equal("FullyFilled")
   }

   @Test
   fun `can handle logical expressions for when cases during read II`() {
      val (vyne, _) = testVyne("""
         model ComplexWhen {
            trader: String?
            initialQuantity: Int?
            quantityStatus: String by when {
                this.trader == "Marty" || this.initialQuantity == 100 -> "ZeroFill"
                else -> "CompleteFill"
            }
         }
      """.trimIndent())
      fun json(trader: String) = """{
      "trader" : "$trader",
      "initialQuantity" : 100
      }
      """.trimMargin()

      val typedObject = TypedInstance.from(vyne.type("ComplexWhen"), json("Marty"), vyne.schema, source = Provided) as TypedObject
      typedObject["quantityStatus"].value.should.equal("ZeroFill")
   }

   @Test
   fun `can handle logical expressions for when cases during read and handle null equality`() {
      val (vyne, _) = testVyne("""
         model ComplexWhen {
            trader: String?
            initialQuantity: Int?
            endQuantity: Int?
            quantityStatus: String by when {
                this.trader == "Marty" && this.initialQuantity == this.endQuantity -> "ZeroFill"
                else -> "CompleteFill"
            }
         }
      """.trimIndent())
      fun json(trader: String) = """{
      "trader" : "$trader"
      }
      """.trimMargin()

      val typedObject = TypedInstance.from(vyne.type("ComplexWhen"), json("Marty"), vyne.schema, source = Provided) as TypedObject
      typedObject["quantityStatus"].value.should.equal("ZeroFill")
   }

   @Test
   fun `can handle null checks`() {
      val (vyne, _) = testVyne("""
         enum PriceType {
            PERCENTAGE,
            BASIS
         }
         model SampleType {
            price: Decimal?
            priceType: PriceType? by when {
                this.price == null -> null
                this.price != null -> jsonPath("${'$'}.type")
            }
         }
      """.trimIndent())
      val json = """{
         "price": null,
         "type": "BASIS"
      }
      """.trimMargin()

      val typedObject = TypedInstance.from(vyne.type("SampleType"), json, vyne.schema, source = Provided) as TypedObject
      typedObject["priceType"].value.should.be.`null`

   }
}

