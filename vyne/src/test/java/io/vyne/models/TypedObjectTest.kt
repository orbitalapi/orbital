package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.winterbe.expekt.expect
import com.winterbe.expekt.should
import io.vyne.models.json.JsonModelParser
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.fail

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

   /**
    * We parse json in two entry points - as a string and
    * as a sequence of JsonNodes.
    * When using the JsonNode approach, ensure everything converts correctly
    */
   @Test
   fun `can parse json as JsonNode`() {
      val jsonNode = jacksonObjectMapper().readTree(traderJson)
      val trader = TypedInstance.from(schema.type("Trader"), jsonNode, schema, source = Provided)
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
      val traderAttributes = jacksonObjectMapper().readValue<Map<String, Any>>(traderJson)
      val instance = TypedObject.fromAttributes(schema.type("Trader"), traderAttributes, schema, source = Provided)
      val raw = instance.toRawObject()
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      JSONAssert.assertEquals(traderJson, rawJson, false);
   }


   @Test
   fun when_unwrappingDatesWithFormats_lenientDateParsingIsUsed() {
      val schema = TaxiSchema.from("""
         type Trade {
            tradeDate : Instant (@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
         }
      """.trimIndent())
      val tradeType = schema.type("Trade")
      fun parseJson(date: String): Instant {
         val json = """{
         |"tradeDate" : "$date"
         |}
      """.trimMargin()
         val typedObject = TypedInstance.from(tradeType, json, schema, source = Provided) as TypedObject
         return typedObject["tradeDate"].value as Instant
      }

      val instant = Instant.parse("2020-05-15T13:00:00Z")
      parseJson("2020-05-15T13:00:00.0Z").should.equal(instant)
      parseJson("2020-05-15T13:00:00.00Z").should.equal(instant)
      parseJson("2020-05-15T13:00:00.000Z").should.equal(instant)
      parseJson("2020-05-15T13:00:00.0000Z").should.equal(instant)
      parseJson("2020-05-15T13:00:00.00000Z").should.equal(instant)
      parseJson("2020-05-15T13:00:00.000000Z").should.equal(instant)
   }

   @Test
   fun whenDateFormatsAreInvalid_then_errorMessageContainsTheExpectedFormat() {
      val schema = TaxiSchema.from("""
         type Trade {
            tradeDate : Instant (@format = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
         }
      """.trimIndent())
      val tradeType = schema.type("Trade")
      val json = """{
         |"tradeDate" : "2020-05-01"
         |}
      """.trimMargin()
      try {
         TypedInstance.from(tradeType, json, schema, source = Provided) as TypedObject
      } catch (e: DataParsingException) {
         e.message.should.equal("Failed to parse value 2020-05-01 to type lang.taxi.Instant(yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z') - Text '2020-05-01' could not be parsed, unparsed text found at index 0")
         return
      }
      fail("Expected an exception to be thrown")


   }

   @Test
   fun `can use default values in json when parsing`() {
      val schema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
            title : Title as String by default("foo")
         }
      """.trimIndent())
      val json = """{ "firstName" : "Jimmy" }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("foo")
   }

   @Test // This specific test because of a bug found where this was failing
   fun `can use default value of empty string in json when parsing`() {
      val schema = TaxiSchema.from("""
         model Person {
            firstName : FirstName as String
            title : Title as String by default("")
         }
      """.trimIndent())
      val json = """{ "firstName" : "Jimmy" }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      instance["title"].value.should.equal("")
   }

   @Test
   fun `can ingest boolean value into enum with synonym`() {
      val schema = TaxiSchema.from("""
         enum LivingOrDead {
            Alive, Dead
         }
         enum IsAlive {
            true synonym of LivingOrDead.Alive,
            false synonym of LivingOrDead.Dead
         }
         model Person {
            name : Name as String
            isAlive : IsAlive
         }
         model OutputPerson {
            name : Name
            livingOrDead : LivingOrDead
         }
      """.trimIndent())
      val (vyne,_) = testVyne(schema)
      val json = """{ "name" : "Bernstein", "isAlive" : false }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      val buildResult = vyne.query().addFact(instance).build("OutputPerson")
      val output = buildResult["OutputPerson"] as TypedObject
      output["livingOrDead"].value!!.should.equal("Dead")
   }

   @Test
   fun `can ingest boolean value into enum with synonym when read using an accessor`() {
      val schema = TaxiSchema.from("""
         enum LivingOrDead {
            Alive, Dead
         }
         enum IsAlive {
            true synonym of LivingOrDead.Alive,
            false synonym of LivingOrDead.Dead
         }
         model Person {
            name : Name as String
            isAlive : IsAlive by jsonPath("$.living")
         }
         model OutputPerson {
            name : Name
            livingOrDead : LivingOrDead
         }
      """.trimIndent())
      val (vyne,_) = testVyne(schema)
      val json = """{ "name" : "Bernstein", "living" : false }"""
      val instance = TypedInstance.from(schema.type("Person"), json, schema, source = Provided) as TypedObject
      val buildResult = vyne.query().addFact(instance).build("OutputPerson")
      val output = buildResult["OutputPerson"] as TypedObject
      output["livingOrDead"].value!!.should.equal("Dead")
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

   @Test
   fun `Downcasted Dates can be unwrapped`() {
      //eventDate : RfqEventDate? (@format = "dd/MMM/yyyy HH:mm:ss") by column("RFQ-Action Date")
      val schema = TaxiSchema.from("""
         type RfqEventDate inherits Date
         type RfqEventTime inherits Time
         type Rfq {
            eventDate : RfqEventDate? (@format = "dd/MMM/yyyy HH:mm:ss")
            eventTime: RfqEventTime (@format = "dd/MMM/yyyy HH:mm:ss")
         }
      """)
      val rfqJson = """
         {
            "eventDate" : "12/Jun/2019 10:20:00",
            "eventTime" : "12/Jun/2019 10:20:00"
         }
      """.trimIndent()
      val rfq = JsonModelParser(schema).parse(schema.type("Rfq"), rfqJson, source = Provided) as TypedObject

      // tradeDateInstant should be an instant
      val eventDate = rfq["eventDate"].value as LocalDate
      eventDate.should.equal(LocalDate.of(2019, 6, 12))
      val eventTime = rfq["eventTime"].value as LocalTime
      eventTime.should.equal(LocalTime.of(10, 20, 0))
      val raw = rfq.toRawObject()

      /// When we write it, the tradeDate should adhere to the format on the type
      val rawJson = jacksonObjectMapper().writeValueAsString(raw)
      val expectedJson = """
         {
           "eventDate" : "2019-06-12",
           "eventTime" : "10:20:00"
         }
         """.trimMargin()
      JSONAssert.assertEquals(expectedJson, rawJson, true);
   }


}
