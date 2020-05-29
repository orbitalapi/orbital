package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.json.JsonModelParser
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant

class TypedObjectTest {


   val traderJson = """
       {
         "username" : "EUR_Trader",
         "jurisdiction" : "EUR",
         "limit" : {
            "currency" : "USD",
            "value" : 100
         }
       }
   """.trimIndent()

   lateinit var schema: TaxiSchema

   @Before
   fun setup() {
      val taxiDef = """
     type Money {
        currency : String
        value : Decimal
    }
    type Trader {
        username : String
        jurisdiction : String
        limit : Money
    }"""

      schema = TaxiSchema.from(taxiDef)
   }

   @Test
   fun canUnwrapTypedObject() {
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson)
      val raw = trader.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun canConvertTypedInstanceToTypeNamedObject() {
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson)
      val raw = trader.toTypeNamedInstance()
      val stringType = "lang.taxi.String".fqn()
      val decimalType = "lang.taxi.Decimal".fqn()
      val expected = TypeNamedInstance(typeName = "Trader".fqn(), value = mapOf(
         "username" to TypeNamedInstance(stringType, "EUR_Trader"),
         "jurisdiction" to TypeNamedInstance(stringType, "EUR"),
         "limit" to TypeNamedInstance("Money".fqn(), mapOf(
            "currency" to TypeNamedInstance(stringType, "USD"),
            "value" to TypeNamedInstance(decimalType, 100.toBigDecimal())
         ))
      ))
      expect(raw).to.equal(expected)
   }

   @Test
   fun canConvertTypedCollectionToTypeNamedObject() {
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson)
      val collection = TypedCollection.arrayOf(schema.type("Trader"), listOf(trader))
      val raw = collection.toTypeNamedInstance() as List<TypeNamedInstance>
      expect(raw).to.have.size(1)
      expect(raw.first().typeName).to.equal("Trader")
   }


   @Test
   fun when_unwrappingDatesWithFormats_then_stringAreReturnedForNonStandard() {
      val schema  = TaxiSchema.from("""
         type TradeDate inherits Instant ( @format = "dd/MM/yy'T'HH:mm:ss" )
         type Trade {
            tradeDate : TradeDate
         }
      """)
      val tradeJson = """
         {
            "tradeDate" : "13/05/20T19:33:22"
         }
      """.trimIndent()
      val trade = JsonModelParser(schema).parse(schema.type("Trade"), tradeJson) as TypedObject
      // tradeDate should be an instant
      val tradeDate = trade["tradeDate"].value as Instant
      tradeDate.should.equal(Instant.parse("2020-05-13T19:33:22Z"))

      val raw = trade.toRawObject()

      /// When we write it, the tradeDate should adhere to the format on the type
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      val expectedJson = """{"tradeDate":"13/05/20T19:33:22"}"""
      JSONAssert.assertEquals(expectedJson, rawJson, true);
   }

}
