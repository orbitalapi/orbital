package com.orbitalhq.protobuf

import com.winterbe.expekt.should
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.SchemaLoader
import io.kotest.matchers.shouldBe
import lang.taxi.generators.protobuf.TaxiGenerator
import okio.FileSystem
import org.junit.Test


// Useful reference from the Square Wire protobuf code:
// https://github.com/square/wire/blob/master/wire-library/wire-schema/src/jvmTest/kotlin/com/squareup/wire/schema/SchemaProtoAdapterTest.kt

class ProtobufFormatDeserializerTest {
   @Test
   fun `can read a simple proto`() {
      val generator = TaxiGenerator(FileSystem.RESOURCES)
         .addSchemaRoot("/simple/src/proto")

      // First, import a protobuf schema to taxi.
      val taxiSchema = TaxiSchema.fromStrings(
         generator.generate(packagesToInclude = listOf("simple")).taxi
      )

      // Grab the protobuf message generator, and create a protobuf message
      val protoSchema = generator.protobufSchema
      val data = mapOf(
         "name" to "Jimmy",
         "id" to 123,
         "email" to "jimmy@demo.com",
         "phones" to listOf(
            mapOf(
               "number" to "+44555-123",
               "type" to "HOME"
            ),
            mapOf(
               "number" to "+44555-456",
               "type" to "WORK"
            )
         )
      )
      val encoded = protoSchema.protoAdapter("simple.Person", false)
         .encode(data)

      // now try and read the protobuf message as a TypedInstance
      val typedInstance = TypedInstance.from(
         taxiSchema.type("simple.Person"), encoded, taxiSchema,
         formatSpecs = listOf(ProtobufFormatSpec)
         ) as TypedObject

      typedInstance.type.fullyQualifiedName.should.equal("simple.Person")
      typedInstance["phones.[0].type"].typeName.should.equal("simple.Person.PhoneType")
      // Make sure all the data structure was deserialized correctly
      typedInstance
         .toRawObject()!!.should.equal(data)

   }

   @Test
   fun `can read a proto generated using a proto library`() {
      val generator = TaxiGenerator(FileSystem.RESOURCES)
         .addSchemaRoot("/cafe/src/proto")
      val protoSchema = generator.protobufSchema
      val data = mapOf(
         "customer_name" to "Jimmy",
         "shots" to listOf(mapOf(
            "bean_type" to "Ground",
            "caffeine_level" to 3.0
         )),
         "foam" to "ZOMG_SO_FOAMY",
         "size_ounces" to 8,
         "dairy" to mapOf(
            "type" to "skim",
            "count" to 1
         )
      )
      val encoded = protoSchema.protoAdapter("CafeDrink", false)
         .encode(data)

      val taxiSchema = TaxiSchema.fromStrings(
         generator.generate().taxi
      )
      // now try and read the protobuf message as a TypedInstance
      val typedInstance = TypedInstance.from(
         taxiSchema.type("CafeDrink"), encoded, taxiSchema,
         formatSpecs = listOf(ProtobufFormatSpec)
      ) as TypedObject
      typedInstance.toRawObject().shouldBe(data)

   }
}
