package com.orbitalhq.connectors.azure.blob

import com.azure.storage.blob.BlobContainerClientBuilder
import com.google.common.io.Resources
import com.winterbe.expekt.should
import com.orbitalhq.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import com.orbitalhq.connectors.azure.blob.registry.InMemoryAzureStoreConnectorRegister
import com.orbitalhq.connectors.azure.blob.support.AzuriteContainer
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayInputStream
import java.util.*

@Testcontainers
class AzureStoreInvocationTest {

   companion object {
      // will be shared between test methods
      @Container
      private val azuriteContainer = AzuriteContainer().withExposedPorts(10000)
   }

   // Azurite default configuration
   private val defaultEndpointsProtocol = "http"
   private val accountName = "devstoreaccount1"
   private val accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
   private val blobEndpoint =
      "http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10000)}/devstoreaccount1"
   private val connectionString =
      "DefaultEndpointsProtocol=$defaultEndpointsProtocol;AccountName=$accountName;AccountKey=$accountKey;BlobEndpoint=$blobEndpoint;"

   private val connectionRegistry = InMemoryAzureStoreConnectorRegister(
      listOf(AzureStorageConnectorConfiguration("movies", connectionString))
   )
   @Test
   fun `check container is running`() {
      azuriteContainer.isRunning.should.be.`true`
   }

   @Test
   fun `can use a TaxiQL statement to query a db`() : Unit = runBlocking {
      val uploadedBlob = uploadTestFileToAzure()
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
            @AzureStoreOperation(container = "${uploadedBlob.container}")
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like)
               }
         }
      """
         )
      ) { schema ->
         listOf(StoreInvoker(AzureStreamProvider(), connectionRegistry, SimpleSchemaProvider(schema)))
      }

      val result = vyne.query("""find { Movie[]( MovieTitle == "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }

   private fun uploadTestFileToAzure(): AzureStoreInfo {
      val containerName = "container-" + UUID.randomUUID()
      val containerClient = BlobContainerClientBuilder()
         .connectionString(connectionString)
         .containerName(containerName)
         .buildClient()
      containerClient.create()

      // create blob and upload text
      val blobName = "blob-" + UUID.randomUUID()
      val blobClient = containerClient.getBlobClient(blobName)
      val content = Resources.getResource("movies.csv").openStream().readAllBytes()
      blobClient.upload(ByteArrayInputStream(content), content.size.toLong())
      return AzureStoreInfo(container = containerName, blob = blobName)
   }

   private data class AzureStoreInfo(val container: String, val blob: String)


}
