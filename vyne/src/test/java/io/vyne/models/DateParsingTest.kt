package io.vyne.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.firstTypedObject
import io.vyne.models.json.addJson
import io.vyne.models.json.parseJson
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import lang.taxi.types.FormatsAndZoneOffset
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.fail

class DateParsingTest {
   @Test
   fun when_unwrappingDatesWithFormats_lenientDateParsingIsUsed() {
      val schema = TaxiSchema.from(
         """
         type Trade {
            @Format("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            tradeDate : Instant
         }
      """.trimIndent()
      )
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
      val schema = TaxiSchema.from(
         """
         type Trade {
            @Format("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            tradeDate : Instant
         }
      """.trimIndent()
      )
      val tradeType = schema.type("Trade")
      val json = """{
         |"tradeDate" : "2020-05-01"
         |}
      """.trimMargin()
      try {
         TypedInstance.from(tradeType, json, schema, source = Provided) as TypedObject
      } catch (e: DataParsingException) {
         e.message.should.contain("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
         return
      }
      fail("Expected an exception to be thrown")
   }


   @Test
   fun whenDateFormatsAreInvalid_then_nullIsReturnedWithMeaningfulError() {
      val schema = TaxiSchema.from(
         """
         type Trade {
            @Format("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            tradeDate : Instant
         }
      """.trimIndent()
      )
      val tradeType = schema.type("Trade")
      val json = """{
         |"tradeDate" : "2020-05-01"
         |}
      """.trimMargin()
      val parsed = TypedInstance.from(
         tradeType,
         json,
         schema,
         source = Provided,
         parsingErrorBehaviour = ParsingFailureBehaviour.ReturnTypedNull
      ) as TypedObject
      val field = parsed["tradeDate"]
      field.should.be.instanceof(TypedNull::class.java)
      val source = field.source as FailedParsingSource
      source.error.should.equal("Failed to parse value 2020-05-01 to type lang.taxi.Instant with formats yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z' - Text '2020-05-01' could not be parsed, unparsed text found at index 0")
   }

   @Test
   fun `default date formats are inherited correctly`() {
      val schema = TaxiSchema.from("""
         type BirthDate inherits Date

         model Person {
            birthDate : BirthDate
         }
      """.trimIndent())
      schema.type("BirthDate").formatAndZoneOffset.should.not.be.`null`
      schema.type("Person").attribute("birthDate").format.should.not.be.`null`
   }

   @Test
   fun `date formats are applied when converting to raw objects`():Unit = runBlocking {
      val (vyne,stub) = testVyne("""
         model Person {
            dateOfBirth : DateOfBirth inherits Date
         }
         model Kiwi {
            @Format("dd-MMM-yy")
            dob : DateOfBirth
         }
      """.trimIndent())
      val person = vyne.parseJson("Person", """{ "dateOfBirth" : "1979-05-10" }""")
      val result = vyne.from(person)
         .build("Kiwi")
         .firstTypedObject()
      val raw = result.toRawObject()
      raw.should.equal(mapOf("dob" to "10-May-79"))
   }

   @Test
   fun when_unwrappingDatesWithFormats_then_stringAreReturnedForNonStandard() {
      val schema = TaxiSchema.from(
         """
          @Format("dd/MM/yy'T'HH:mm:ss" )
         type TradeDateInstant inherits Instant

         @Format("MM-dd-yyyy" )
         type TradeDateDate inherits Date

         @Format("dd/MM/yyyy HH:mm:ss" )
         type TradeDateDateTime inherits DateTime
         type Trade {
            tradeDateInstant : TradeDateInstant
            tradeDateDate : TradeDateDate
            tradeDateDateTime : TradeDateDateTime
         }
      """
      )
      val tradeJson = """
         {
            "tradeDateInstant" : "13/05/20T19:33:22",
            "tradeDateDate" : "12-06-2019",
            "tradeDateDateTime" : "15/07/2020 21:33:22"
         }
      """.trimIndent()

      val trade = TypedInstance.from(schema.type("Trade"), tradeJson, schema) as TypedObject

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
   fun `can parse string to date`() {
      val schema = TaxiSchema.from(
         """
         type NearLegDate inherits Date
         type FarLegDate inherits Date
         type OrderId inherits String
         model Order {
            orderId : OrderId by jsonPath("$.orderId")
            @Format("dd/MMM/yyyy")
            nearLegDate : NearLegDate by left(jsonPath("$.eventDate"), indexOf(jsonPath("$.eventDate"),";"))
            @Format("dd/MMM/yyyy")
            farLegDate : FarLegDate by right(jsonPath("$.eventDate"), indexOf(jsonPath("$.eventDate"),";") + 1)
         }
         model OutputOrder {
            orderId : OrderId
            @Format("yyyy-MM-dd")
            nearDate : NearLegDate
            @Format("yyyy-MM-dd")
            farDate : FarLegDate
         }
      """.trimIndent()
      )
      val instance = TypedInstance.from(
         schema.type("Order"),
         """{ "orderId" : "abc", "eventDate" : "10/MAY/2021;10/MAY/2031" } """,
         schema,
         source = Provided
      )
      val (vyne, _) = testVyne(schema)
      runBlocking {
         val result = vyne.from(instance).build("OutputOrder")
         val outputOrder = result.firstTypedObject()
         val json = jacksonObjectMapper().writeValueAsString(outputOrder.toRawObject())
         val expected = """{"orderId":"abc","nearDate":"2021-05-10","farDate":"2031-05-10"}"""
         JSONAssert.assertEquals(expected, json, true)
      }
   }


   @Test
   fun `can parse string to date with csv`() {
      val schema = TaxiSchema.from(
         """
         type NearLegDate inherits Date
         type FarLegDate inherits Date
         type OrderId inherits String
         @CsvList
         type alias OrderList as Order[]

         model Order {
            orderId : OrderId by column(1)
            @Format("dd/MMM/yyyy")
            nearLegDate : NearLegDate by left(column(2), indexOf(column(2),";"))
            @Format("dd/MMM/yyyy")
            farLegDate : FarLegDate by right(column(2), indexOf(column(2),";") + 1)
         }
         model OutputOrder {
            orderId : OrderId
            @Format("yyyy-MM-dd")
            nearDate : NearLegDate
            @Format("yyyy-MM-dd")
            farDate : FarLegDate
         }
      """.trimIndent()
      )
      val csv = """orderId,settlementDate
abc,10/MAY/2021;10/MAY/2031
      """.trimIndent()
      val instance = TypedInstance.from(schema.type("OrderList"), csv, schema, source = Provided)
      val (vyne, _) = testVyne(schema)
      runBlocking {
         val result = vyne.from(instance).build("OutputOrder")
         val outputOrder = result.firstTypedObject()
         val json = jacksonObjectMapper().writeValueAsString(outputOrder.toRawObject())
         val expected = """{"orderId":"abc","nearDate":"2021-05-10","farDate":"2031-05-10"}"""
         JSONAssert.assertEquals(expected, json, true)
      }
   }

   @Test
   fun `Downcasted Dates can be unwrapped`() {
      //eventDate : RfqEventDate? @Format("dd/MMM/yyyy HH:mm:ss") by column("RFQ-Action Date")
      val schema = TaxiSchema.from(
         """
         type RfqEventDate inherits Date
         type RfqEventTime inherits Time
         type Rfq {
            @Format("dd/MMM/yyyy HH:mm:ss")
            eventDate : RfqEventDate?
             @Format("dd/MMM/yyyy HH:mm:ss")
            eventTime: RfqEventTime
         }
      """
      )
      val rfqJson = """
         {
            "eventDate" : "12/Jun/2019 10:20:00",
            "eventTime" : "12/Jun/2019 10:20:00"
         }
      """.trimIndent()
      val rfq = TypedInstance.from(schema.type("Rfq"), rfqJson, schema, source = Provided) as TypedObject

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


   @Test
   fun `can read zoned instants`() {
      val schema = TaxiSchema.from(
         """
            @Format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
         type EventDate inherits Instant
      """.trimIndent()
      )

      val inputStrings = listOf(
         "2021-06-07T08:41:04.555+00:00",
         "2021-06-07T09:41:04.555+01:00",
         "2021-06-07T07:41:04.555-01:00"
      )
      val expected = Instant.parse("2021-06-07T08:41:04.555Z")

      inputStrings.forEach { sourceDateString ->
         val parsedInstant =
            TypedInstance.from(schema.type("EventDate"), sourceDateString, schema, source = Provided).value as Instant
         parsedInstant.epochSecond.should.equal(expected.epochSecond)
      }
   }

   @Test
   fun `timezone formatting examples`() {

      listOf(
         "2021-06-07T08:41:04.551555+01:00" to Instant.parse("2021-06-07T07:41:04.551555Z"),
         "2021-06-07T08:41:04.551+01:00" to Instant.parse("2021-06-07T07:41:04.551Z"),
         "2021-06-07T08:41:04.551Z" to Instant.parse("2021-06-07T08:41:04.551Z"),
         "2021-06-07T08:41:04Z" to Instant.parse("2021-06-07T08:41:04Z"),
      ).validateAgainstDateTimePattern("yyyy-MM-dd'T'HH:mm:ss[.S]XXX")

      listOf(
         "2021-06-07T08:41:04.551+0100" to Instant.parse("2021-06-07T07:41:04.551Z"),
      ).validateAgainstDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX")

      listOf(
         "1979-03-02T08:41:04.551Z" to Instant.parse("1979-03-02T08:41:04.551Z"),
         "1979-03-02T08:41:04.551-0100" to Instant.parse("1979-03-02T09:41:04.551Z"),
         "1979-03-02T08:41:04.551+0100" to Instant.parse("1979-03-02T07:41:04.551Z"),
      ).validateAgainstDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX")


   }

   @Test
   fun `when timezone information isnt present then utc is inferred`() {
      val schema = TaxiSchema.from(
         """
          @Format("yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // Note there's no timezone here
         type EventDate inherits Instant
      """.trimIndent()
      )
      val parsed = TypedInstance.from(
         schema.type("EventDate"),
         "2021-06-07T08:41:04.555",
         schema,
         source = Provided
      ).value as Instant
      parsed.should.equal(Instant.parse("2021-06-07T08:41:04.555Z"))
   }

}


private fun List<Pair<String, Instant>>.validateAgainstDateTimePattern(pattern: String): Unit {
   val schema = TaxiSchema.from(
      """
         @Format("$pattern")
         type EventDate inherits Instant
      """.trimIndent()
   )
   this.forEach { (inputString, expected) ->
      val parsed = TypedInstance.from(
         schema.type("EventDate"),
         inputString,
         schema,
         source = Provided,
         format = FormatsAndZoneOffset.forFormat(pattern)
      ).value as Instant
      parsed.should.equal(expected)
   }

}

