package io.vyne.models.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import io.vyne.models.Provided
import io.vyne.schemas.Type
import lang.taxi.types.JsonPathAccessor
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParserTest() {

   private val objectMapper: ObjectMapper = jacksonObjectMapper()
   lateinit var primitiveParser: PrimitiveParser;
   lateinit var parser: JsonAttributeAccessorParser

   @Before
   fun setup() {
      primitiveParser = mock();

      parser = JsonAttributeAccessorParser(primitiveParser)
   }

   @Test
   fun parseInteger() {
      val accessor = JsonPathAccessor("/age")

      val node = objectMapper.readTree(""" {  "age": 1 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1), any(), eq(Provided))
   }

   @Test
   fun parseDouble() {
      val accessor = JsonPathAccessor("/age")

      val node = objectMapper.readTree(""" {  "age": 1.609 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1.609), any(), eq(Provided))
   }

   @Test
   fun parseIntegerAsString() {
      val accessor = JsonPathAccessor("/age")

      val node = objectMapper.readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("1"), any(), eq(Provided))

   }

   @Test
   fun parseFieldDoesntExist() {
      val accessor = JsonPathAccessor("/year")

      val node = objectMapper.readTree(""" {  "age": "1" } """) as ObjectNode

      val value = parser.parseToType(mock(), accessor, node, mock(), Provided)
      value.value.should.be.`null`

   }

   @Test
   fun parseEnum() {
      val accessor = JsonPathAccessor("/country")

      val node = objectMapper.readTree(""" {  "country": "France" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum
      parser.parseToType(enumMock, accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("France"), any(), eq(Provided))

   }

   @Test
   fun parseEmptyEnum() {
      val accessor = JsonPathAccessor("/country")

      val node = objectMapper.readTree(""" {  "country": "" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum

      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
      instance.value.should.be.`null`

      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }

   @Test
   fun jsonPathParseInteger() {
      val accessor = JsonPathAccessor("$.age")

      val node = objectMapper.readTree(""" {  "age": 1 } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1), any(), eq(Provided))
   }

   @Test
   fun jsonPathParseDouble() {
      val accessor = JsonPathAccessor("$.age")

      val node = objectMapper.readTree(""" {  "age": 1.609 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1.609), any(), eq(Provided))
   }

   @Test
   fun jsonPathParseIntegerAsString() {
      val accessor = JsonPathAccessor("$.age")

      val node = objectMapper.readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("1"), any(), eq(Provided))

   }

   @Test
   fun jsonPathParseFieldDoesntExist() {
      val accessor = JsonPathAccessor("$.year")

      val node = objectMapper.readTree(""" {  "age": "1" } """) as ObjectNode

      val value = parser.parseToType(mock(), accessor, node, mock(), Provided)
      value.value.should.be.`null`

   }

   @Test
   fun jsonPathParseEnum() {
      val accessor = JsonPathAccessor("$.country")

      val node = objectMapper.readTree(""" {  "country": "France" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum
      parser.parseToType(enumMock, accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("France"), any(), eq(Provided))


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

      val typeRef: TypeReference<HashMap<String?, Any?>?> = object : TypeReference<HashMap<String?, Any?>?>() {}
      val map = objectMapper.readValue(json, typeRef)
      val accessor = JsonPathAccessor("$.annaJson.Attributes.ReferenceRate")
      val referenceRateMock = mock<Type>()
      doReturn(false).whenever(referenceRateMock).isEnum
      parser.parseToType(referenceRateMock, accessor, map!!, mock(), Provided)
      verify(primitiveParser).parse(eq("EUR-EURIBOR-Reuters"), any(), eq(Provided))

      parser.parseToType(referenceRateMock, JsonPathAccessor("$.annaJson.Attributes.ExpiryDate "), map!!, mock(), Provided)
      verify(primitiveParser).parse(eq("2022-09-13"), any(), eq(Provided))

      parser.parseToType(referenceRateMock, JsonPathAccessor("$.annaJson.Attributes.NotionalCurrency "), map!!, mock(), Provided)
      verify(primitiveParser).parse(eq("EUR"), any(), eq(Provided))
   }

   @Test
   @Ignore("Is this valid?  Passing empty string is not a valid enum value.")
   fun jsonPathParseEmptyEnum() {
      val accessor = JsonPathAccessor("$.country")

      val node = objectMapper.readTree(""" {  "country": "" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum

      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
      instance.value.should.be.`null`

      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }




}

