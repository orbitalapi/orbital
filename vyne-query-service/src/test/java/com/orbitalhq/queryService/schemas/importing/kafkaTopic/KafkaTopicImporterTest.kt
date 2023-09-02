package com.orbitalhq.queryService.schemas.importing.kafkaTopic

import com.winterbe.expekt.should
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.cockpit.core.schemas.importing.SchemaConversionRequest
import com.orbitalhq.cockpit.core.schemas.importing.concatenatedSource
import com.orbitalhq.cockpit.core.schemas.importing.kafka.KafkaTopicConverterOptions
import com.orbitalhq.cockpit.core.schemas.importing.kafka.KafkaTopicImporter
import com.orbitalhq.connectors.kafka.KafkaConnectorTaxi
import com.orbitalhq.queryService.schemas.importing.BaseSchemaConverterServiceTest
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import org.junit.Test

class KafkaTopicImporterTest : BaseSchemaConverterServiceTest() {

   val schemaProvider = SimpleSchemaProvider(
      TaxiSchema.from(
         """
         model Person {
            firstName : String
            lastName : String
         }
      """.trimIndent()
      )
   )

   @Test
   fun `generates expected taxi`() {

      val importer = KafkaTopicImporter(schemaProvider)
      val generated = importer.convert(
         SchemaConversionRequest(
            KafkaTopicImporter.SUPPORTED_FORMAT,
            packageIdentifier = PackageIdentifier.fromId("foo/test/1.0.0")
         ),
         KafkaTopicConverterOptions(
            "my-kafka-connection",
            "my-topic",
            KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST,
            "Person".fqn(),
            "com.orbitalhq.test.kafka",
         )
      ).block()
      val expected = """
         namespace com.orbitalhq.test.kafka {
            @com.orbitalhq.kafka.KafkaService(connectionName = "my-kafka-connection")
            service MyKafkaConnectionService {
               @com.orbitalhq.kafka.KafkaOperation(topic = "my-topic" , offset = "earliest")
               operation consumeFromMyTopic(  ) : Stream<Person>
            }
         }
      """

      // Can't use compileTheSameAs here, since we don't support imported sources (eg., Person)
      // in that asserter
      generated!!.concatenatedSource.withoutWhitespace().should.equal(expected.withoutWhitespace())
   }

   @Test
   fun `can convert kafka topic to operation`() {

      val importer = KafkaTopicImporter(schemaProvider)
      val converterService = createConverterService(importer, schemaProvider = schemaProvider)

      val result = converterService.preview(
         SchemaConversionRequest(
            KafkaTopicImporter.SUPPORTED_FORMAT,
            KafkaTopicConverterOptions(
               "my-kafka-connection",
               "my-topic",
               KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST,
               "Person".fqn(),
               "io.foo.test.kafka",
            ),
            packageIdentifier = PackageIdentifier.fromId("foo/test/1.0.0")
         )
      ).block()
      result.services.should.have.size(1)
      result.services.single().operations.should.have.size(1)
   }
}
