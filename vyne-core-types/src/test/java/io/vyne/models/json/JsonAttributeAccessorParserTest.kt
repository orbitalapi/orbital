package io.vyne.models.json

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import junit.framework.Assert.fail
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
      val accessor = XpathAccessor("/age")

      var node = jacksonObjectMapper().readTree(""" {  "age": 1 } """) as ObjectNode


      parser.parseToType(mock(), accessor, node, mock())

      verify(primitiveParser).parse(eq("1"), any(), any())
   }

   @Test
   fun parseIntegerAsString() {
      val accessor = XpathAccessor("/age")

      var node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      parser.parseToType(mock(), accessor, node, mock())

      verify(primitiveParser).parse(eq("1"), any(), any())

   }

   @Test
   fun parseFieldDoesntExist() {
      val accessor = XpathAccessor("/year")

      var node = jacksonObjectMapper().readTree(""" {  "age": "1" } """) as ObjectNode

      try{
         parser.parseToType(mock(), accessor, node, mock())
         fail()
      }catch(e: IllegalStateException) {
         e.message.should.be.equal("Could not find xpath /year in record")
      }

   }
}

