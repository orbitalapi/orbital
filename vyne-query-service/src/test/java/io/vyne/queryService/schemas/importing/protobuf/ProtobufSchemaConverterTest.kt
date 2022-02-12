package io.vyne.queryService.schemas.importing.protobuf

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.http.MockWebServerRule
import io.vyne.queryService.schemas.importing.BaseSchemaConverterServiceTest
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
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

      val conversionResponse = converterService.import(
         SchemaConversionRequest(
            ProtobufSchemaConverter.PROTOBUF_FORMAT,
            ProtobufSchemaConverterOptions(
               url = server.url("/sample.proto").toString()
            )
         )
      ).block(Duration.ofSeconds(100))!!

      conversionResponse.types.should.have.size(1)
   }

   @Test
   fun `can convert protobuf with package name from http`() {
      val converterService = createConverterService(converter)
      val protobuf = Resources.getResource("schemas/protobuf/simple-schema.proto").readText()
      server.prepareResponse { response -> response.setBody(protobuf) }

      val conversionResponse = converterService.import(
         SchemaConversionRequest(
            ProtobufSchemaConverter.PROTOBUF_FORMAT,
            ProtobufSchemaConverterOptions(
               url = server.url("/sample.proto").toString()
            )
         )
      ).block(Duration.ofSeconds(100))!!

      conversionResponse.types.should.have.size(4)
   }

}
