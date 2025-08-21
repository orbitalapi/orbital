package com.orbitalhq.connectors.kafka

import com.orbitalhq.annotations.streaming.StreamingQueryAnnotations
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class KafkaQueryOptionsTest {
   val schema = TaxiSchema.fromStrings(
      StreamingQueryAnnotations.schema,
      KafkaConnectorTaxi.schema,
      """
               ${KafkaConnectorTaxi.Annotations.imports}
               type MovieId inherits String
               type MovieTitle inherits String

               model Movie {
                  id : MovieId
                  title : MovieTitle
               }

               @KafkaService( connectionName = "moviesConnection" )
               service MovieService {
                  @KafkaOperation( topic = "movies", offset = "earliest" )
                  stream streamMovieQuery:Stream<Movie>
               }
      """.trimIndent()
   )
   @Test
   fun `can override consumer group and offset using annotations`() {
      val (vyne) = testVyne(schema)
      val (service, operation) = schema.remoteOperation("MovieService@@streamMovieQuery".fqn())

      val (_,defaultParseOptions: QueryOptions) = vyne.parseQuery("""
         find { Movie }
      """.trimIndent())

      KafkaInvoker.createConsumerRequest(service,operation,defaultParseOptions).let { consumerRequest ->
         // This is the default as defined on the schema
         consumerRequest.offset.shouldBe(KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST)
         consumerRequest.streamSourceId.shouldBeNull()
      }


      val (_,parseOptionsWithConsumerId: QueryOptions) = vyne.parseQuery("""
         import com.orbitalhq.streams.StreamConsumer
         @StreamConsumer( id = "my-consumer" )
         find { Movie }
      """.trimIndent())

      KafkaInvoker.createConsumerRequest(service,operation,parseOptionsWithConsumerId).let { consumerRequest ->
         // This is the default as defined on the schema
         consumerRequest.offset.shouldBe(KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.EARLIEST)
         consumerRequest.streamSourceId.shouldBe("my-consumer")
      }

      val (_,parseOptionsWithConsumerIdAndOffset: QueryOptions) = vyne.parseQuery("""
         import com.orbitalhq.streams.StreamConsumer
         @StreamConsumer( id = "my-consumer", offset = "latest" )
         find { Movie }
      """.trimIndent())

      KafkaInvoker.createConsumerRequest(service,operation,parseOptionsWithConsumerIdAndOffset).let { consumerRequest ->
         // This is the default as defined on the schema
         consumerRequest.streamSourceId.shouldBe("my-consumer")
         consumerRequest.offset.shouldBe(KafkaConnectorTaxi.Annotations.KafkaOperation.Offset.LATEST)
      }
   }
}
