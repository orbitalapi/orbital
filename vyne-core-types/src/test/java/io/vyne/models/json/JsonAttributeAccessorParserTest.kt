package io.vyne.models.json

import com.nhaarman.mockitokotlin2.mock
import com.winterbe.expekt.should
import io.vyne.models.PrimitiveParser
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Parses a single attribute at defined xpath accessor
 */
class JsonAttributeAccessorParserTest {
   lateinit var primitiveParser: PrimitiveParser
   lateinit var parser: JsonAttributeAccessorParser

   @Before
   fun setup() {
      primitiveParser = mock()
      parser = JsonAttributeAccessorParser(primitiveParser)
   }

   @Test
   fun parseInteger() {
      val schema = TaxiSchema.from("""
         model Foo {
            age : Int by jsonPath("/age")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{ "age": 1 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1)
   }


   @Test
   fun parseFieldDoesntExist() {
      val schema = TaxiSchema.from("""
         model Foo {
            age : Int by jsonPath("/year")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{ "age": 1 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.be.`null`
   }

   @Test
   fun parseEnum() {
      val schema = TaxiSchema.from("""
         enum Country { France }
         model Foo {
            country : Country by jsonPath("/country")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{  "country": "France" }""", schema, source = Provided) as TypedObject
      instance["country"].value.should.equal("France")
   }

   @Test
   @Ignore("Parsing empty string to enums isn't supported")
   fun parseEmptyEnum() {
      val schema = TaxiSchema.from("""
         enum Country { France }
         model Foo {
            country : Country by jsonPath("/country")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{  "country": "" }""", schema, source = Provided) as TypedObject
   }

   @Test
   fun jsonPathParseInteger() {
      val schema = TaxiSchema.from("""
         model Foo {
            age : Int by jsonPath("$.age")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{ "age": 1 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.equal(1)
   }



   @Test
   fun jsonPathParseFieldDoesntExist() {
      val schema = TaxiSchema.from("""
         model Foo {
            age : Int by jsonPath("$.year")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{ "age": 1 } """, schema, source = Provided) as TypedObject
      instance["age"].value.should.be.`null`
   }

   @Test
   fun jsonPathParseEnum() {
      val schema = TaxiSchema.from("""
         enum Country { France }
         model Foo {
            country : Country by jsonPath("$.country")
         }
      """.trimIndent()

      )
      val instance = TypedInstance.from(schema.type("Foo"), """{  "country": "France" }""", schema, source = Provided) as TypedObject
      instance["country"].value.should.equal("France")

   }


   @Test
   @Ignore("Is this valid?  Passing empty string is not a valid enum value.")
   fun jsonPathParseEmptyEnum() {
//      val accessor = JsonPathAccessor("$.country")
//
//      val node = objectMapper.readTree(""" {  "country": "" } """) as ObjectNode
//
//      val enumMock = mock<Type>()
//      doReturn(true).whenever(enumMock).isEnum
//
//      val instance = parser.parseToType(enumMock, accessor, node, mock(), Provided)
//      instance.value.should.be.`null`
//
//      verify(primitiveParser, never()).parse(any(), any(), eq(Provided))

   }




}

