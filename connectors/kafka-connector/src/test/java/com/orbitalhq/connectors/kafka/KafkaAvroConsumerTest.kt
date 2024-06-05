package com.orbitalhq.connectors.kafka

import com.orbitalhq.PackageIdentifier
import com.orbitalhq.PackageMetadata
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.asVersionedSource
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import lang.taxi.generators.avro.AvroAnnotationSchema
import lang.taxi.generators.avro.TaxiGenerator
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.Encoder
import org.apache.avro.io.EncoderFactory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.io.ByteArrayOutputStream
import java.io.OutputStream

@SpringBootTest(classes = [KafkaQueryTestConfig::class])
@RunWith(SpringRunner::class)
class KafkaAvroConsumerTest : BaseKafkaContainerTest() {

   @Before
   override fun before() {
      super.before()
      val (producer, registry) = buildProducer()
      kafkaProducer = producer
      connectionRegistry = registry
   }

   val avroSchema = """{
  "type": "record",
  "name": "Movie",
  "namespace": "movies",
  "fields": [
    {
      "name": "id",
      "type": "string"
    },
    {
      "name": "title",
      "type": ["null", "string"],
      "default": null
    }
  ]
}
"""

   @Test
   fun `can use a TaxiQL statement to consume avro JSON message from a Kafka stream`(): Unit = runBlocking {
      // Combine the Avro source, with the service declared below
      val sourcePackage = avroToSourcePackage(avroSchema).let {
         it.copy(sources = it.sources + VersionedSource.sourceOnly(moviesServiceTaxi))
      }

      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(sourcePackage)
      sendMessage(avroJsonMessage("message1", "Star Wars"))
      sendMessage(avroJsonMessage("message2", "Empire Strikes Back"))


      val result = vyne.query(
         """
         stream { movies.Movie }"""
            .trimIndent()
      )

         .results.take(2)
         .timeout(kotlin.time.Duration.parse("20s"))
         .toList() as List<TypedObject>
      result.should.have.size(2)
      val rawMaps = result.map { it.toRawObject() as Map<String, Any> }
      rawMaps.single { it["id"] == "message1" }.shouldBe(
         mapOf(
            "id" to "message1", "title" to "Star Wars"
         )
      )
   }

   @Test
   fun `can use a TaxiQL statement to consume avro byte array message from a Kafka stream`(): Unit = runBlocking {
      // Combine the Avro source, with the service declared below
      val sourcePackage = avroToSourcePackage(avroSchema).let {
         it.copy(sources = it.sources + VersionedSource.sourceOnly(moviesServiceTaxi))
      }
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(sourcePackage)
      sendMessage(avroBytesMessage("message1", "Star Wars"))
      sendMessage(avroBytesMessage("message2", "Empire Strikes Back"))


      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(2)
         .timeout(kotlin.time.Duration.parse("20s"))
         .toList() as List<TypedObject>
      result.should.have.size(2)
      val rawMaps = result.map { it.toRawObject() as Map<String, Any> }
      rawMaps.single { it["id"] == "message1" }.shouldBe(
         mapOf(
            "id" to "message1", "title" to "Star Wars"
         )
      )
   }

   protected fun avroBytesMessage(id: String, title: String): ByteArray {
      return avroMessage(id, title) { schema, outputStream -> EncoderFactory.get().binaryEncoder(outputStream, null) }
   }

   protected fun avroJsonMessage(id: String, title: String): ByteArray {
      return avroMessage(id, title) { schema, outputStream -> EncoderFactory.get().jsonEncoder(schema, outputStream) }
   }

   // generates an avro schema without using taxi.
   // Use this to test deserialization
   private fun avroMessage(id: String, title: String, encoderProvider: (Schema, OutputStream) -> Encoder): ByteArray {
      val avroSchema = Schema.Parser().parse(avroSchema)
      val outputStream = ByteArrayOutputStream()
      val encoder = encoderProvider(avroSchema, outputStream)
      val movie: GenericRecord = GenericData.Record(avroSchema).apply {
         put("id", id)
         put("title", title)
      }
      val writer = GenericDatumWriter<Any>(avroSchema)
      writer.write(movie, encoder)
      encoder.flush()
      outputStream.close()
      return outputStream.toByteArray()
   }

   private val moviesServiceTaxi = """
               import lang.taxi.formats.AvroMessage
               import lang.taxi.formats.AvroField
               ${KafkaConnectorTaxi.Annotations.imports}

               namespace movies {
                  @KafkaService( connectionName = "moviesConnection" )
                  service MovieService {
                     @KafkaOperation( topic = "movies", offset = "earliest" )
                     stream streamMovieQuery:Stream<Movie>
                  }
               }
               ${AvroAnnotationSchema.taxi}
            """.trimIndent()
}

private fun avroToSourcePackage(avro: String): SourcePackage {
   val generatedTaxi = TaxiGenerator().generate(avro, "source.avro")
   val identifier = PackageIdentifier.fromId("com.foo/test/1.0.0")
   val sourcePackage = SourcePackage.asTranspiledPackage(
      PackageMetadata.from(identifier),
      listOf(VersionedSource.unversioned("source.avro", avro)),
      generatedTaxi.asVersionedSource(identifier, "avro"),
      generatedTaxi.sourceMap
   )
   return sourcePackage
}
