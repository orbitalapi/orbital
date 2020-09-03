package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.json.JsonModelParser
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson, source = Provided)
      val raw = trader.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun canParseJsonUsingTypedInstanceFrom() {
      val trader = TypedInstance.from(schema.type("Trader"), traderJson, schema, source = Provided)
      val raw = trader.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);

   }

   @Test
   fun canConvertTypedInstanceToTypeNamedObject() {
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson, source = Provided)
      val raw = trader.toTypeNamedInstance()
      val stringType = "lang.taxi.String".fqn()
      val decimalType = "lang.taxi.Decimal".fqn()
      val expected = TypeNamedInstance(typeName = "Trader".fqn(), value = mapOf(
         "username" to TypeNamedInstance(stringType, "EUR_Trader", Provided),
         "jurisdiction" to TypeNamedInstance(stringType, "EUR", Provided),
         "limit" to TypeNamedInstance("Money".fqn(), mapOf(
            "currency" to TypeNamedInstance(stringType, "USD", Provided),
            "value" to TypeNamedInstance(decimalType, 100.toBigDecimal(), Provided)
         ), Provided)
      ), source = Provided)
      expect(raw).to.equal(expected)
   }

   @Test
   fun canConvertTypedCollectionToTypeNamedObject() {
      val trader = JsonModelParser(schema).parse(schema.type("Trader"), traderJson, source = Provided)
      val collection = TypedCollection.arrayOf(schema.type("Trader"), listOf(trader))
      val raw = collection.toTypeNamedInstance() as List<TypeNamedInstance>
      expect(raw).to.have.size(1)
      expect(raw.first().typeName).to.equal("Trader")
   }

   @Test
   fun canParseFromNestedMapToTypedObject() {
      val traderAttributes = jacksonObjectMapper().readValue<Map<String,Any>>(traderJson)
      val instance = TypedObject.fromAttributes(schema.type("Trader"), traderAttributes, schema, source = Provided)
      val raw = instance.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun when_unwrappingDatesWithFormats_then_stringAreReturnedForNonStandard() {
      val schema = TaxiSchema.from("""
         type TradeDateInstant inherits Instant ( @format = "dd/MM/yy'T'HH:mm:ss" )
         type TradeDateDate inherits Date ( @format = "MM-dd-yyyy" )
         type TradeDateDateTime inherits DateTime ( @format = "dd/MM/yyyy HH:mm:ss" )
         type Trade {
            tradeDateInstant : TradeDateInstant
            tradeDateDate : TradeDateDate
            tradeDateDateTime : TradeDateDateTime
         }
      """)
      val tradeJson = """
         {
            "tradeDateInstant" : "13/05/20T19:33:22",
            "tradeDateDate" : "12-06-2019",
            "tradeDateDateTime" : "15/07/2020 21:33:22"
         }
      """.trimIndent()
      val trade = JsonModelParser(schema).parse(schema.type("Trade"), tradeJson, source = Provided) as TypedObject

      // tradeDateInstant should be an instant
      val tradeDateInstant = trade["tradeDateInstant"].value as Instant
      tradeDateInstant.should.equal(Instant.parse("2020-05-13T19:33:22Z"))

      // tradeDateDate should be an date
      val tradeDateDate = trade["tradeDateDate"].value as LocalDate
      tradeDateDate.should.equal(LocalDate.of(2019, 12, 6))

      // tradeDateDateTime should be an date
      val tradeDateDateTime = trade["tradeDateDateTime"].value as LocalDateTime
      tradeDateDateTime.should.equal(LocalDateTime.of(2020, 7, 15, 21, 33, 22))

      val raw = trade.toRawObject()

      /// When we write it, the tradeDate should adhere to the format on the type
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      val expectedJson = """
         {
           "tradeDateInstant":"13/05/20T19:33:22",
           "tradeDateDate":"12-06-2019",
           "tradeDateDateTime":"15/07/2020 21:33:22"
         }

         """.trimMargin()
      JSONAssert.assertEquals(expectedJson, rawJson, true);
   }

}
