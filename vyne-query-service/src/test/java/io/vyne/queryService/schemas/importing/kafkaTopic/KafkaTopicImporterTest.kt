package io.vyne.queryService.schemas.importing.kafkaTopic

import com.winterbe.expekt.should
import io.vyne.connectors.kafka.KafkaConnectorTaxi
import io.vyne.queryService.schemas.importing.BaseSchemaConverterServiceTest
import io.vyne.queryService.schemas.importing.SchemaConversionRequest
import io.vyne.queryService.schemas.importing.kafka.KafkaTopicConverterOptions
import io.vyne.queryService.schemas.importing.kafka.KafkaTopicImporter
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.fqn
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.withoutWhitespace
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
         SchemaConversionRequest(KafkaTopicImporter.SUPPORTED_FORMAT),
         KafkaTopicConverterOptions(
            "my-kafka-connection",
            "my-topic",
            KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST,
            "Person".fqn(),
            "io.vyne.test.kafka",
         )
      ).block()
      val expected = """
         namespace io.vyne.test.kafka {
            @io.vyne.kafka.KafkaService(connectionName = "my-kafka-connection")
            service MyKafkaConnectionService {
               @io.vyne.kafka.KafkaOperation(topic = "my-topic" , offset = "earliest")
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

      val result = converterService.import(SchemaConversionRequest(
         KafkaTopicImporter.SUPPORTED_FORMAT,
         KafkaTopicConverterOptions(
            "my-kafka-connection",
            "my-topic",
            KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST,
            "Person".fqn(),
            "io.vyne.test.kafka",
         )
      )).block()
      result.services.should.have.size(1)
      result.services.single().operations.should.have.size(1)
   }
}
