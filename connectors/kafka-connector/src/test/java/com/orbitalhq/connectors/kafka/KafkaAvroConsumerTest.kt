package com.orbitalhq.connectors.kafka

import com.orbitalhq.avro.AvroFormatSpec
import com.orbitalhq.models.TypedObject
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.winterbe.expekt.should
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import lang.taxi.generators.avro.AvroAnnotationSchema
import lang.taxi.generators.avro.AvroMessageAnnotation
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
import java.time.Duration

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

   @Test
   fun `can use a TaxiQL statement to consume avro JSON message from a Kafka stream`(): Unit = runBlocking {
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(defaultSchemaSrc)
      sendMessage(avroJsonMessage("message1", "Star Wars"))
      sendMessage(avroJsonMessage("message2", "Empire Strikes Back"))


      val result = vyne.query(
         """
         stream { Movie }"""
            .trimIndent()
      )

         .results.take(2)
         .timeout(kotlin.time.Duration.parse("20s"))
         .toList() as List<TypedObject>
      result.should.have.size(2)
      val rawMaps = result.map { it.toRawObject() as Map<String,Any>}
      rawMaps.single { it["id"] == "message1" }.shouldBe(mapOf(
         "id" to "message1", "title" to "Star Wars"
      ))
   }
   @Test
   fun `can use a TaxiQL statement to consume avro byte array message from a Kafka stream`(): Unit = runBlocking {
      val (vyne, kafkaStreamManager) = vyneWithKafkaInvoker(defaultSchemaSrc)
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
      val rawMaps = result.map { it.toRawObject() as Map<String,Any>}
      rawMaps.single { it["id"] == "message1" }.shouldBe(mapOf(
         "id" to "message1", "title" to "Star Wars"
      ))
   }

   protected fun avroBytesMessage(id: String, title: String): ByteArray {
      return avroMessage(id, title) { schema, outputStream -> EncoderFactory.get().binaryEncoder(outputStream, null) }
   }

   protected fun avroJsonMessage(id: String, title: String): ByteArray {
      return avroMessage(id, title) { schema, outputStream -> EncoderFactory.get().jsonEncoder(schema, outputStream) }
   }

   private fun avroMessage(id: String, title: String, encoderProvider: (Schema, OutputStream) -> Encoder): ByteArray {
      val schemaCache = AvroFormatSpec.newSchemaCache()
      val avroSchema = schemaCache.get(defaultSchema.type("movies.Movie") to defaultSchema)
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

   private val defaultSchemaSrc = """
               import lang.taxi.formats.AvroMessage
               import lang.taxi.formats.AvroField
               ${KafkaConnectorTaxi.Annotations.imports}

               namespace movies {
                  type MovieId inherits String
                  type MovieTitle inherits String

                  @AvroMessage
                  model Movie {
                     @AvroField(ordinal = 1) id : MovieId
                     // Making this field nullable forces the generated Avro JSON
                     // to be non-standard json, as the type becomes
                     // union { null , string }
                     @AvroField(ordinal = 2) title : MovieTitle?
                  }

                  @KafkaService( connectionName = "moviesConnection" )
                  service MovieService {
                     @KafkaOperation( topic = "movies", offset = "earliest" )
                     stream streamMovieQuery:Stream<Movie>
                  }
               }
               ${AvroAnnotationSchema.taxi}
            """.trimIndent()

   private val defaultSchema = TaxiSchema.from(defaultSchemaSrc)


}
