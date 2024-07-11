package com.orbitalhq.avro

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.UndefinedSource
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import lang.taxi.generators.avro.AvroMessageAnnotation
import lang.taxi.generators.avro.TaxiGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant

class AvroFormatSerializerTest {
   // Define the schema
   val avroSchemaJson = """
    {
      "type": "record",
      "name": "TestMessage",
      "namespace": "com.example.test",
      "doc": "A record to test various Avro features",
      "fields": [
        {"name": "id", "type": "string", "doc": "A unique identifier for the message"},
        {"name": "timestamp", "type": {"type": "long", "logicalType": "timestamp-millis"}, "doc": "Timestamp of the message"},
        {"name": "name", "type": ["null", "string"], "default": null, "doc": "Name of the entity, nullable"},
        {"name": "age", "type": ["null", "int"], "default": null, "doc": "Age of the entity, nullable"},
        {"name": "status", "type": {"type": "enum", "name": "Status", "symbols": ["ACTIVE", "INACTIVE", "DELETED"]}, "doc": "Status of the entity"},
        {"name": "tags", "type": {"type": "array", "items": "string"}, "doc": "A list of tags associated with the entity"},
        {"name": "metadata", "type": {"type": "map", "values": "string"}, "doc": "A map of metadata key-value pairs"},
        {
          "name": "address",
          "type": {"type": "record", "name": "Address", "fields": [
            {"name": "street", "type": "string", "doc": "Street address"},
            {"name": "city", "type": "string", "doc": "City"},
            {"name": "zip_code", "type": "string", "doc": "Postal code"}
          ]},
          "doc": "Nested record representing an address"
        },
        {"name": "is_active", "type": "boolean", "default": true, "doc": "Indicates if the entity is active"},
        {"name": "score", "type": ["null", "double"], "default": null, "doc": "Score of the entity, nullable"}
      ]
    }
    """

   val avroSchemaWithRootArrayJson = """
    {
      "type": "array",
      "items" : {
         "type" : "record",
         "name": "TestMessage",
         "namespace": "com.example.test",
         "doc": "A record to test various Avro features",
         "fields": [
           {"name": "id", "type": "string", "doc": "A unique identifier for the message"},
           {"name": "timestamp", "type": {"type": "long", "logicalType": "timestamp-millis"}, "doc": "Timestamp of the message"},
           {"name": "name", "type": ["null", "string"], "default": null, "doc": "Name of the entity, nullable"},
           {"name": "age", "type": ["null", "int"], "default": null, "doc": "Age of the entity, nullable"},
           {"name": "status", "type": {"type": "enum", "name": "Status", "symbols": ["ACTIVE", "INACTIVE", "DELETED"]}, "doc": "Status of the entity"},
           {"name": "tags", "type": {"type": "array", "items": "string"}, "doc": "A list of tags associated with the entity"},
           {"name": "metadata", "type": {"type": "map", "values": "string"}, "doc": "A map of metadata key-value pairs"},
           {
             "name": "address",
             "type": {"type": "record", "name": "Address", "fields": [
               {"name": "street", "type": "string", "doc": "Street address"},
               {"name": "city", "type": "string", "doc": "City"},
               {"name": "zip_code", "type": "string", "doc": "Postal code"}
             ]},
             "doc": "Nested record representing an address"
           },
           {"name": "is_active", "type": "boolean", "default": true, "doc": "Indicates if the entity is active"},
           {"name": "score", "type": ["null", "double"], "default": null, "doc": "Score of the entity, nullable"}
         ]
       }
    }
    """

   val testJson = """{
  "id": "12345",
  "timestamp": 1654027200000,
  "name": "John Doe",
  "age": 30,
  "status": "ACTIVE",
  "tags": ["test", "avro"],
  "metadata": {
    "key1": "value1",
    "key2": "value2"
  },
  "address": {
    "street": "123 Main St",
    "city": "Anytown",
    "zip_code": "12345"
  },
  "is_active": true,
  "score": 99.5
}"""

   @BeforeEach
   fun setup() {
      AvroFormatSpec.clearCache()
   }
   @AfterEach
   fun tearDown() {
      AvroFormatSpec.clearCache()
   }
   @Test
   fun `can read and write a message with avro`() {
      val avroSourcePackage = avroToSourcePackage(avroSchemaJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage"), testJson, schema)
      val schemaCache = AvroSchemaCache()
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      bytes.shouldBeInstanceOf<ByteArray>()

      val readTypedInstance = AvroFormatDeserializer(schemaCache).parse(
         bytes,
         typedInstance.type,
         typedInstance.type.getMetadata(AvroMessageAnnotation.NAME.fqn()),
         schema,
         UndefinedSource
      )

      typedInstance.toRawObject()
         .shouldBe((readTypedInstance as TypedInstance).toRawObject())
   }

   @Test
   fun `can read and write a message with an array at root with avro`() {
      val schemaCache = AvroSchemaCache()
      val avroSourcePackage = avroToSourcePackage(avroSchemaWithRootArrayJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage[]"), "[ $testJson ]", schema)
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      bytes.shouldBeInstanceOf<ByteArray>()

      val readTypedInstance = AvroFormatDeserializer(schemaCache).parse(
         bytes,
         typedInstance.type,
         typedInstance.type.collectionType!!.getMetadata(AvroMessageAnnotation.NAME.fqn()),
         schema,
         UndefinedSource
      )

      typedInstance.toRawObject()
         .shouldBe((readTypedInstance as TypedInstance).toRawObject())
   }

   @Test
   fun `can read and write a message with an array at root with avro using TypedInstance parser`() {
      val schemaCache = AvroSchemaCache()
      val avroSourcePackage = avroToSourcePackage(avroSchemaWithRootArrayJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage[]"), "[ $testJson ]", schema)
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      bytes.shouldBeInstanceOf<ByteArray>()

      val readTypedInstance = AvroFormatDeserializer(schemaCache).parse(
         bytes,
         typedInstance.type,
         typedInstance.type.collectionType!!.getMetadata(AvroMessageAnnotation.NAME.fqn()),
         schema,
         UndefinedSource
      )

      typedInstance.toRawObject()
         .shouldBe((readTypedInstance as TypedInstance).toRawObject())
   }

   @Test
   @Disabled("Not supported - see ORB-479")
   fun `can read a message that inherits from an avro message`() {
      // Write the test message
      val schemaCache = AvroSchemaCache()
      val avroSourcePackage = avroToSourcePackage(avroSchemaWithRootArrayJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage[]"), "[ $testJson ]", schema)
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      // Now, deserialize as a subtype
      val schema2 = TaxiSchema.from("""
          model TestMessageWithMetadata inherits TestMessage {
            timestamp : Instant = now()
          }

      """.trimIndent(), importSources = listOf(schema))


      val readTypedInstance = TypedInstance.from(schema2.type("TestMessageWithMetadata"), bytes, schema2,
         formatSpecs = listOf(AvroFormatSpec)
      )
      readTypedInstance.shouldNotBeNull()
   }

   @Test
   fun `can read a message that composes an avro message array`() {
      // Write the test message
      val schemaCache = AvroSchemaCache()
      val avroSourcePackage = avroToSourcePackage(avroSchemaWithRootArrayJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage[]"), "[ $testJson ]", schema)
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      // Now, deserialize as a subtype
      val schema2 = TaxiSchema.from("""
          model TestMessageWithMetadata  {
            timestamp : Instant = now()
            message:  TestMessage[]
          }

      """.trimIndent(), importSources = listOf(schema))
      val mergedSchema = schema2.merge(schema)


      val readTypedInstance = TypedInstance.from(mergedSchema.type("TestMessageWithMetadata"), bytes, mergedSchema,
         formatSpecs = listOf(AvroFormatSpec)
      ) as TypedObject
      readTypedInstance.shouldNotBeNull().shouldBeInstanceOf<TypedObject>()
      readTypedInstance["timestamp"].toRawObject().shouldNotBeNull()
      readTypedInstance["message"].toRawObject().shouldNotBeNull()
   }

   @Test
   fun `can read a message that composes an avro message`() {
      val avroSourcePackage = avroToSourcePackage(avroSchemaJson)
      val schema = TaxiSchema.from(avroSourcePackage)
      val typedInstance = TypedInstance.from(schema.type("com.example.test.TestMessage"), testJson, schema)
      val schemaCache = AvroSchemaCache()
      val bytes = AvroFormatSerializer(schemaCache).write(typedInstance, mock {  }, schema, -1)

      // Now, deserialize as a subtype
      val schema2 = TaxiSchema.from("""
          model TestMessageWithMetadata  {
            timestamp : Instant = now()
            message:  TestMessage
          }

      """.trimIndent(), importSources = listOf(schema))
      val mergedSchema = schema2.merge(schema)


      val readTypedInstance = TypedInstance.from(mergedSchema.type("TestMessageWithMetadata"), bytes, mergedSchema,
         formatSpecs = listOf(AvroFormatSpec)
      ) as TypedObject
      readTypedInstance.shouldNotBeNull().shouldBeInstanceOf<TypedObject>()
      readTypedInstance["timestamp"].toRawObject().shouldNotBeNull()
      readTypedInstance["message"].toRawObject().shouldNotBeNull()
   }

   private fun avroToSourcePackage(avro:String):SourcePackage {
      val generatedTaxi = TaxiGenerator().generate(avro, "source.avro")
      val identifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
      val sourcePackage = SourcePackage.asTranspiledPackage(
         PackageMetadata.from(identifier),
         listOf(VersionedSource.unversioned("source.avro",avro)),
         generatedTaxi.asVersionedSource(identifier, "avro"),
         generatedTaxi.sourceMap
      )
      return sourcePackage
   }

}
