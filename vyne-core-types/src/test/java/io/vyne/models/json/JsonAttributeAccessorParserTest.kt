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

      var node = jacksonObjectMapper().readTree(""" {  "age": 1 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1), any(), eq(Provided))
   }

   @Test
   fun parseDouble() {
      val accessor = JsonPathAccessor("/age")

      var node = jacksonObjectMapper().readTree(""" {  "age": 1.609 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq(1.609), any(), eq(Provided))
   }

   @Test
   fun parseIntegerAsString() {
      val accessor = JsonPathAccessor("/age")

      var node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("1"), any(), eq(Provided))

   }

   @Test
   fun parseFieldDoesntExist() {
      val accessor = JsonPathAccessor("/year")

      var node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      try{
         parser.parseToType(mock(), accessor, node, mock(), Provided)
         fail()
      }catch(e: IllegalStateException) {
         e.message.should.be.equal("Could not find json pointer /year in record")
      }

   }

   @Test
   fun parseEnum() {
      val accessor = JsonPathAccessor("/country")

      var node = jacksonObjectMapper().readTree(""" {  "country": "France" } """) as ObjectNode

      var enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum
      parser.parseToType(enumMock, accessor, node, mock(), Provided)

      verify(primitiveParser).parse(eq("France"), any(), eq(Provided))

   }

   @Test
   fun parseEmptyEnum() {
      val accessor = JsonPathAccessor("/country")

      var node = jacksonObjectMapper().readTree(""" {  "country": "" } """) as ObjectNode

      var enumMock = mock<Type>()
      doReturn(true).whenever(enumMock).isEnum

      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
      instance.value.should.be.`null`

      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }
}

