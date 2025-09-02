package com.orbitalhq.models.conversion

import com.winterbe.expekt.should
import com.orbitalhq.models.ConversionService
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

class VyneConversionServiceTest {

   @Test
   fun `when converter is on classpath then it is used`() {
      val converter = ConversionService.DEFAULT_CONVERTER
      converter.should.be.instanceof(VyneConversionService::class.java)
   }

   @Test
   fun parseDouble() {
      val schema = TaxiSchema.from(
         """
         model Foo {
            age : Decimal by jsonPath("/age")
         }
      """.trimIndent()

      )
      val instance =
         TypedInstance.from(schema.type("Foo"), """{ "age": 1.609 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1.609.toBigDecimal())
   }

   @Test
   fun parseIntegerAsString() {
      val schema = TaxiSchema.from(
         """
         model Foo {
            age : Int by jsonPath("/age")
         }
      """.trimIndent()

      )
      val instance =
         TypedInstance.from(schema.type("Foo"), """{ "age": "1" } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1)

   }

   @Test
   fun `complex type regression`() {
      val json = """
         {
           "isin": "EZ4G5P8L56T7",
           "annaJson": {
             "Header": {
               "AssetClass": "Rates",
               "InstrumentType": "Swap",
               "UseCase": "Cross_Currency_Basis",
               "Level": "InstRefDataReporting"
             },
             "ISIN": {
               "ISIN": "EZ4G5P8L56T7",
               "Status": "New",
               "StatusReason": "",
               "LastUpdateDateTime": "2020-11-11T13:45:21"
             },
             "Attributes": {
               "NotionalCurrency": "EUR",
               "ExpiryDate": "2022-09-13",
               "TermofContractValue": 2,
               "TermofContractUnit": "YEAR",
               "ReferenceRate": "EUR-EURIBOR-Reuters",
               "ReferenceRateTermValue": 3,
               "ReferenceRateTermUnit": "MNTH",
               "OtherNotionalCurrency": "USD",
               "OtherLegReferenceRate": "USD-LIBOR-BBA",
               "OtherLegReferenceRateTermValue": 3,
               "OtherLegReferenceRateTermUnit": "MNTH",
               "NotionalSchedule": "Constant",
               "DeliveryType": "PHYS",
               "PriceMultiplier": 1
             },
             "Derived": {
               "FullName": "Rates Swap Cross_Currency_Basis 2 YEAR EURUSD EUR-EURIBOR-Reuters 3 MNTH USD-LIBOR-BBA 3 MNTH 20220913",
               "ClassificationType": "SRACCP",
               "CommodityDerivativeIndicator": "FALSE",
               "UnderlyingAssetType": "Basis Swap (Float - Float)",
               "SingleorMultiCurrency": "Cross Currency",
               "IssuerorOperatoroftheTradingVenueIdentifier": "NA",
               "ShortName": "NA/Swap Flt Flt EUR USD 20220913",
               "ISOReferenceRate": "EURI",
               "ISOOtherLegReferenceRate": "LIBO"
             },
             "TemplateVersion": 2
           }
         }
      """.trimIndent()

      val schema = TaxiSchema.from(
         """
         model Foo {
            referenceRate : String by jsonPath("$.annaJson.Attributes.ReferenceRate")
            expiryDate : Date by jsonPath("$.annaJson.Attributes.ExpiryDate")
            notionalCurrency : String by jsonPath("$.annaJson.Attributes.NotionalCurrency")
         }
      """.trimIndent()
      )
      val instance = TypedInstance.from(schema.type("Foo"), json, schema, source = Provided) as TypedObject
      instance["referenceRate"].value.should.equal("EUR-EURIBOR-Reuters")
      instance["expiryDate"].value.should.equal(LocalDate.parse("2022-09-13"))
      instance["notionalCurrency"].value.should.equal("EUR")
   }

   @Test
   fun jsonPathParseIntegerAsString() {
      val schema = TaxiSchema.from(
         """
         model Foo {
            age : Int by jsonPath("$.age")
         }
      """.trimIndent()

      )
      val instance =
         TypedInstance.from(schema.type("Foo"), """{ "age": "1" } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1)
   }

   @Test
   fun jsonPathParseDouble() {
      val schema = TaxiSchema.from(
         """
         model Foo {
            age : Decimal by jsonPath("$.age")
         }
      """.trimIndent()

      )
      val instance =
         TypedInstance.from(schema.type("Foo"), """{ "age": 1.609 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1.609.toBigDecimal())
   }

   @Test
   fun `parse java date to LocalDAte`() {
      val schema = TaxiSchema.from("""
         model Person {
            dob: DateOfBirth inherits Date
         }
      """.trimIndent())
      val localDate = LocalDate.of(2024, 3, 2)
      val instance = TypedInstance.from(
         schema.type("Person"),
         mapOf("dob" to  Date.from(localDate.atStartOfDay(ZoneOffset.UTC).toInstant())),
         schema
      ) as TypedObject
      instance["dob"].value.shouldBe(localDate)
   }
   @Test
   fun `parse java date to LocalTime`() {
      val schema = TaxiSchema.from("""
      model Meeting {
         startTime: StartTime inherits Time
      }
   """.trimIndent())

      val localTime = LocalTime.of(13, 45, 30)
      val date = Date.from(
         localTime.atDate(LocalDate.of(1970, 1, 1))
            .atZone(ZoneId.systemDefault())
            .toInstant()
      )

      val instance = TypedInstance.from(
         schema.type("Meeting"),
         mapOf("startTime" to date),
         schema
      ) as TypedObject

      instance["startTime"].value.shouldBe(localTime)
   }

   @Test
   fun `parse java date to LocalDateTime`() {
      val schema = TaxiSchema.from("""
      model Event {
         timestamp: Timestamp inherits DateTime
      }
   """.trimIndent())

      val localDateTime = LocalDateTime.of(2024, 3, 2, 14, 30, 15, 123_000_000)
      val date = Date.from(localDateTime.toInstant(ZoneOffset.UTC))

      val instance = TypedInstance.from(
         schema.type("Event"),
         mapOf("timestamp" to date),
         schema
      ) as TypedObject

      instance["timestamp"].value.shouldBe(localDateTime)
   }

   @Test
   fun `parse java date to Instant`() {
      val schema = TaxiSchema.from("""
      model LogEntry {
         createdAt: CreatedAt inherits Instant
      }
   """.trimIndent())

      val instant = Instant.parse("2024-03-02T14:30:15.123Z")
      val date = Date.from(instant)

      val instance = TypedInstance.from(
         schema.type("LogEntry"),
         mapOf("createdAt" to date),
         schema
      ) as TypedObject

      instance["createdAt"].value.shouldBe(instant)
   }

}
