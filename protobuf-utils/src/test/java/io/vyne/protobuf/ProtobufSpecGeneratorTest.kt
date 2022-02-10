package io.vyne.protobuf

import com.winterbe.expekt.should
import io.vyne.protobuf.wire.RepoBuilder
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
import lang.taxi.generators.protobuf.TaxiGenerator
import okio.FileSystem
import org.junit.Test

class ProtobufSpecGeneratorTest {
   val coffeeSchema = RepoBuilder()
      .add(
         "coffee.proto",
         """
          |message CafeDrink {
          |  optional string customer_name = 1;
          |  repeated EspressoShot shots = 2;
          |  optional Foam foam = 3;
          |  optional int32 size_ounces = 14;
          |  optional Dairy dairy = 15;
          |
          |  enum Foam {
          |    NOT_FOAMY_AND_QUITE_BORING = 1;
          |    ZOMG_SO_FOAMY = 3;
          |  }
          |}
          |
          |message Dairy {
          |  optional int32 count = 2;
          |  optional string type = 1;
          |}
          |
          |message EspressoShot {
          |  optional string bean_type = 1;
          |  optional double caffeine_level = 2;
          |}
          """.trimMargin()
      )
      .schema()

   @Test
   fun `generates simple protobuf spec`() {
      val generatedTaxi = TaxiGenerator()
         .generate(protobufSchema = coffeeSchema)
      val schema = TaxiSchema.fromStrings(generatedTaxi.taxi)
      val generatedProto = ProtobufSpecGenerator(schema).generateProtobufSrc(schema.type("CafeDrink"))

      // Assert that we've created multiple files with the correct content.
      // Note that in protobuf, like in java, file names are expected to match
      // directory paths.
      generatedProto.map { (path, protoSpec) -> path.toString() to protoSpec.withoutWhitespace() }
         .toMap()
         .should.equal(
            mapOf(
               "CafeDrink.proto" to
                  """syntax = "proto3";

import "EspressoShot.proto";
import "CafeDrink/Foam.proto";
import "Dairy.proto";

message CafeDrink {
  string customer_name = 1;
  repeated EspressoShot shots = 2;
  CafeDrink.Foam foam = 3;
  int32 size_ounces = 14;
  Dairy dairy = 15;
}""".withoutWhitespace(),
               "EspressoShot.proto" to """syntax = "proto3";

message EspressoShot {
  string bean_type = 1;
  double caffeine_level = 2;
}""".withoutWhitespace(),
               "CafeDrink/Foam.proto" to """
               syntax = "proto3";
               package CafeDrink;

               enum Foam {
                 NOT_FOAMY_AND_QUITE_BORING = 1;
                 ZOMG_SO_FOAMY = 3;
               }
            """.trimIndent().withoutWhitespace(),
               "Dairy.proto" to """
               syntax = "proto3";

               message Dairy {
                 int32 count = 2;
                 string type = 1;
               }
            """.trimIndent().withoutWhitespace()
            )
         )
   }

   @Test
   fun `generates valid protobuf for sample schema`() {
      val generator = TaxiGenerator(fileSystem = FileSystem.RESOURCES)
      generator.addSchemaRoot("/simple/src/proto")
      val generatedTaxiCode = generator.generate(packagesToInclude = listOf("simple"))
      val taxiSchema = TaxiSchema.fromStrings(generatedTaxiCode.taxi)
      val protobufSchema = ProtobufSpecGenerator(taxiSchema).generateProtobufSchema(taxiSchema.type("simple.Person"))
   }

   @Test
   fun `generates protobuf schema`() {
      val generatedTaxi = TaxiGenerator()
         .generate(protobufSchema = coffeeSchema)
      val schema = TaxiSchema.fromStrings(generatedTaxi.taxi)
      val generatedProto = ProtobufSpecGenerator(schema).generateProtobufSchema(schema.type("CafeDrink"))
   }
}
