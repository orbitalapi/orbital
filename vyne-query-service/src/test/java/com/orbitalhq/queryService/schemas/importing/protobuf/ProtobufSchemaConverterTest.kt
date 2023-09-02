package com.orbitalhq.queryService.schemas.importing.protobuf

import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConversionRequest
import com.orbitalhq.cockpit.core.schemas.importing.protobuf.ProtobufSchemaConverter
import com.orbitalhq.cockpit.core.schemas.importing.protobuf.ProtobufSchemaConverterOptions
import com.orbitalhq.http.MockWebServerRule
import com.orbitalhq.queryService.schemas.importing.BaseSchemaConverterServiceTest
import org.junit.Rule
import org.junit.Test
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProtobufSchemaConverterTest : BaseSchemaConverterServiceTest() {
   @Rule
   @JvmField
   final val server = MockWebServerRule()

   val converter = ProtobufSchemaConverter()

   @Test
   fun `can convert protobuf without package name from http`() {
      val converterService = createConverterService(converter)
      val protobuf = """syntax = "proto3";

message Person {
   string name = 1;
   int32 id = 2;
   string email = 3;
}"""
      server.prepareResponse { response -> response.setBody(protobuf) }

      val conversionResponse = converterService.preview(
         SchemaConversionRequest(
            ProtobufSchemaConverter.PROTOBUF_FORMAT,
            ProtobufSchemaConverterOptions(
               url = server.url("/sample.proto").toString()
            ),
            packageIdentifier = PackageIdentifier.fromId("foo/test/1.0.0")
         )
      ).block(Duration.ofSeconds(5))!!

      conversionResponse.types.should.have.size(1)
   }

   @Test
   fun `can convert protobuf with package name from http`() {
      val converterService = createConverterService(converter)
      val protobuf = Resources.getResource("schemas/protobuf/simple-schema.proto").readText()
      server.prepareResponse { response -> response.setBody(protobuf) }

      val conversionResponse = converterService.preview(
         SchemaConversionRequest(
            ProtobufSchemaConverter.PROTOBUF_FORMAT,
            ProtobufSchemaConverterOptions(
               url = server.url("/sample.proto").toString()
            ),
            packageIdentifier = PackageIdentifier.fromId("foo/test/1.0.0")
         )
      ).block(Duration.ofSeconds(5))!!

      conversionResponse.types.should.have.size(4)
   }

}
