package com.orbitalhq.connectors.azure.blob

import com.google.common.collect.Streams
import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import com.orbitalhq.connectors.azure.blob.registry.InMemoryAzureStoreConnectorRegister
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventBroker
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.query.tracing.ObjectStoreRequest
import com.orbitalhq.query.tracing.ObjectStoreResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.Disabled
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

class StoreInvokerTest {
   private val connectionRegistry = InMemoryAzureStoreConnectorRegister(
      listOf(AzureStorageConnectorConfiguration("movies", "connectionStr"))
   )
   @Test
   @Ignore
   fun `emits tracing events when querying Azure blob store and includes request payload`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            AzureStoreConnectionTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${AzureStoreConnectionTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

          @com.orbitalhq.formats.Csv(
                     delimiter = ",",
                     nullValue = "NULL"
                  )
         model Movie {
            id : MovieId by column(1)
            title : MovieTitle by column(2)
         }

         @BlobService( connectionName = "movies" )
         service MovieDb {
            @AzureStoreOperation(container = "bucket1")
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like)
               }
         }
      """
         )
      ) { schema ->
         val inputStream = Resources.getResource("movies.csv").openStream()
         val parser =  CSVParser.parse(inputStream, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())
         val typedInstanceStream =  Streams.stream(parser.iterator()).map {
            TypedInstance.from(
               schema.type("Movie"),
               it,
               schema
            )
         }

         listOf(StoreInvoker(SimpleStreamProvider(typedInstanceStream), connectionRegistry, SimpleSchemaProvider(schema)))
        }

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Movie[](MovieTitle == "A New Hope") } """,
         eventBroker = eventBroker
      ).rawObjects()

      // Should have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify request event
      val requestEvent = eventSink.collectedEvents.first()
      requestEvent.spanState.shouldBe(SpanState.ACTIVE)
      val requestMetadata = requestEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreRequest>()
      requestEvent.eventVerb.shouldBe("Query")
      val requestPayload = requestMetadata.payload()
      requestPayload.shouldNotBeNull()
      requestPayload.shouldContain("SELECT")
      requestPayload.shouldContain("MovieTitle")

      // Verify response event
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreResponse>()
      responseEvent.eventVerb.shouldBe("Query response")

      result.should.have.size(1)
      result.first().should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }

   @Test
   @Ignore
   fun `emits tracing events when no results found in Azure blob store`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            AzureStoreConnectionTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${AzureStoreConnectionTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

          @com.orbitalhq.formats.Csv(
                     delimiter = ",",
                     nullValue = "NULL"
                  )
         model Movie {
            id : MovieId by column(1)
            title : MovieTitle by column(2)
         }

         @BlobService( connectionName = "movies" )
         service MovieDb {
            @AzureStoreOperation(container = "bucket1")
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like)
               }
         }
      """
         )
      ) { schema ->
         val inputStream = Resources.getResource("movies.csv").openStream()
         val parser =  CSVParser.parse(inputStream, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())
         val typedInstanceStream =  Streams.stream(parser.iterator()).map {
            TypedInstance.from(
               schema.type("Movie"),
               it,
               schema
            )
         }

         listOf(StoreInvoker(SimpleStreamProvider(typedInstanceStream), connectionRegistry, SimpleSchemaProvider(schema)))
        }

      val (eventBroker, eventSink) = QueryContextEventBroker.withTestTraceSpan()

      val result = vyne.query(
         """find { Movie[](MovieTitle == "Non-existent Movie") } """,
         eventBroker = eventBroker
      ).rawObjects()

      // Should still have request and response events
      eventSink.collectedEvents.shouldHaveSize(2)

      // Verify response event for no results
      val responseEvent = eventSink.collectedEvents.last()
      responseEvent.spanState.shouldBe(SpanState.COMPLETE)
      val responseMetadata = responseEvent.exchangeMetadata.shouldBeInstanceOf<ObjectStoreResponse>()
      responseMetadata.size.shouldBe(0)

      val responsePayload = responseMetadata.payload()
      responsePayload.shouldNotBeNull()
      responsePayload.shouldContain("Retrieved 0 records")

      result.should.have.size(0)
   }

   @Test
   @Ignore("Failing, and not currently used, so not investigating")
   fun `can use a TaxiQL statement to query a db`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            AzureStoreConnectionTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${AzureStoreConnectionTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

          @com.orbitalhq.formats.Csv(
                     delimiter = ",",
                     nullValue = "NULL"
                  )
         model Movie {
            id : MovieId by column(1)
            title : MovieTitle by column(2)
         }

         @BlobService( connectionName = "movies" )
         service MovieDb {
            @AzureStoreOperation(container = "bucket1")
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like)
               }
         }
      """
         )
      ) { schema ->
         val inputStream = Resources.getResource("movies.csv").openStream()
         val parser =  CSVParser.parse(inputStream, StandardCharsets.UTF_8, CSVFormat.DEFAULT.withFirstRecordAsHeader())
         val typedInstanceStream =  Streams.stream(parser.iterator()).map {
            TypedInstance.from(
               schema.type("Movie"),
               it,
               schema
            )
         }

         listOf(StoreInvoker(SimpleStreamProvider(typedInstanceStream), connectionRegistry, SimpleSchemaProvider(schema)))
        }

      val result = vyne.query("""find { Movie[]( MovieTitle == "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }
}
