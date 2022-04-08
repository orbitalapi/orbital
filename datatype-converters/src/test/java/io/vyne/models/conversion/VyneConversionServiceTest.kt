package io.vyne.models.conversion

import com.winterbe.expekt.should
import io.vyne.models.ConversionService
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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

}
