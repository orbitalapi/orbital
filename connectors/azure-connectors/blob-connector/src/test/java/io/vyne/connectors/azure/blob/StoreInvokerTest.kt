package io.vyne.connectors.azure.blob

import com.google.common.collect.Streams
import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import io.vyne.connectors.azure.blob.registry.InMemoryAzureStoreConnectorRegister
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.stream.Stream

class StoreInvokerTest {
   private val connectionRegistry = InMemoryAzureStoreConnectorRegister(
      listOf(AzureStorageConnectorConfiguration("movies", "connectionStr"))
   )
   @Test
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

          @io.vyne.formats.Csv(
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

      val result = vyne.query("""findAll { Movie[]( MovieTitle == "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }
}
