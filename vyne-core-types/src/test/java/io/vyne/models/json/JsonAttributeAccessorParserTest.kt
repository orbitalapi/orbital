package io.vyne.models.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.*
import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import io.vyne.models.Provided
import io.vyne.schemas.Type
import junit.framework.Assert.fail
import lang.taxi.types.JsonPathAccessor
import lang.taxi.types.XpathAccessor
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParserTest() {

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

      val node = jacksonObjectMapper().readTree(""" {  "age": 1 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1), any(), eq(Provided))
   }

   @Test
   fun parseDouble() {
      val accessor = JsonPathAccessor("/age")

      val node = jacksonObjectMapper().readTree(""" {  "age": 1.609 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1.609), any(), eq(Provided))
   }

   @Test
   fun parseIntegerAsString() {
      val accessor = JsonPathAccessor("/age")

      val node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("1"), any(), eq(Provided))

   }

   @Test
   fun parseFieldDoesntExist() {
      val accessor = JsonPathAccessor("/year")

      val node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      val value = parser.parseToType(mock(), accessor, node, mock(), Provided)
      value.value.should.be.`null`

   }

   @Test
   fun parseEnum() {
      val accessor = JsonPathAccessor("/country")

      val node = jacksonObjectMapper().readTree(""" {  "country": "France" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum
      parser.parseToType(enumMock, accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("France"), any(), eq(Provided))

   }

   @Test
   fun parseEmptyEnum() {
      val accessor = JsonPathAccessor("/country")

      val node = jacksonObjectMapper().readTree(""" {  "country": "" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum

      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
      instance.value.should.be.`null`

      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }

   @Test
   fun jsonPathParseInteger() {
      val accessor = JsonPathAccessor("$.age")

      val node = jacksonObjectMapper().readTree(""" {  "age": 1 } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1), any(), eq(Provided))
   }

   @Test
   fun jsonPathParseDouble() {
      val accessor = JsonPathAccessor("$.age")

      val node = jacksonObjectMapper().readTree(""" {  "age": 1.609 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1.609), any(), eq(Provided))
   }

   @Test
   fun jsonPathParseIntegerAsString() {
      val accessor = JsonPathAccessor("$.age")

      val node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("1"), any(), eq(Provided))

   }

   @Test
   fun jsonPathParseFieldDoesntExist() {
      val accessor = JsonPathAccessor("$.year")

      val node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      val value = parser.parseToType(mock(), accessor, node, mock(), Provided)
      value.value.should.be.`null`

   }

   @Test
   fun jsonPathParseEnum() {
      val accessor = JsonPathAccessor("$.country")

      val node = jacksonObjectMapper().readTree(""" {  "country": "France" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum
      parser.parseToType(enumMock, accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("France"), any(), eq(Provided))

   }

   @Test
   @Ignore("Is this valid?  Passing empty string is not a valid enum value.")
   fun jsonPathParseEmptyEnum() {
      val accessor = JsonPathAccessor("$.country")

      val node = jacksonObjectMapper().readTree(""" {  "country": "" } """) as ObjectNode

      val enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum

      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
      instance.value.should.be.`null`

      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }


}

