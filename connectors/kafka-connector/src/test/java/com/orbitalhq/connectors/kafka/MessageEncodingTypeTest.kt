package com.orbitalhq.connectors.kafka

import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class MessageEncodingTypeTest {

   @Test
   fun `message with nothing declared is a string`() {
      val schema = TaxiSchema.from("""
         model Person {
            name : String
         }
      """.trimIndent())
      MessageEncodingType.forType(schema.type("Person")).shouldBe(MessageEncodingType.STRING)
   }

   @Test
   fun `message with avro is byte array`() {
      val schema = TaxiSchema.from("""
         @lang.taxi.formats.AvroMessage
         model Person {
            name : String
         }
      """.trimIndent())
      MessageEncodingType.forType(schema.type("Person")).shouldBe(MessageEncodingType.BYTE_ARRAY)
   }

   @Test
   fun `message composing byte array message is decoded as byte array`() {
      val schema = TaxiSchema.from("""
         @lang.taxi.formats.AvroMessageWrapper
         model PersonMessage {
            @KafkaMessageKey
            messageId : String
            person:Person
         }

         @lang.taxi.formats.AvroMessage
         model Person {
            name : String
         }
      """.trimIndent())
      MessageEncodingType.forType(schema.type("PersonMessage")).shouldBe(MessageEncodingType.BYTE_ARRAY)
   }
}
