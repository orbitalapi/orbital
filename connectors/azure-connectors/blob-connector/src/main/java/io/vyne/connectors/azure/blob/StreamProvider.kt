package io.vyne.connectors.azure.blob

import com.azure.storage.blob.BlobServiceClientBuilder
import io.vyne.connectors.azure.blob.StreamProvider.Companion.typedInstanceStream
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.csv.CsvFormatFactory
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.csv.CsvFormatSpecAnnotation
import io.vyne.models.format.FormatDetector
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import reactor.core.scheduler.Schedulers
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import java.util.stream.StreamSupport

interface StreamProvider {
   fun stream(targetType: Type,
              schema: Schema,
              azureStoreConnection: AzureStorageConnectorConfiguration,
              containerName: String,
              blobName: String?): Stream<TypedInstance>
   fun formatDetector(): FormatDetector {
      return FormatDetector.get(listOf(CsvFormatSpec))
   }

   companion object {
      fun typedInstanceStream(
         inputStream: InputStream,
         targetType: Type,
         schema: Schema,
         formatDetector: FormatDetector): Stream<TypedInstance> {
         val csvModelFormatAnnotation = formatDetector.getFormatType(targetType)?.let { if (it.second is CsvFormatSpec) CsvFormatSpecAnnotation.from(it.first) else null }
         return if (csvModelFormatAnnotation != null) {
            val csvFormat = CsvFormatFactory.fromParameters(csvModelFormatAnnotation.ingestionParameters)
            val parser = csvFormat.parse(inputStream.bufferedReader())
            StreamSupport.stream(parser.spliterator(), false)
               .map { csvRecord ->
                  TypedInstance.from(
                     targetType,
                     csvRecord,
                     schema,
                     source = Provided
                  )
               }
         } else {
            val linesStream = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
            linesStream.map { line ->
               TypedInstance.from(
                  targetType,
                  line,
                  schema,
                  source = Provided
               )
            }
         }
      }
   }
}

class AzureStreamProvider : StreamProvider {
   override fun stream(targetType: Type,
                       schema: Schema,
                       azureStoreConnection: AzureStorageConnectorConfiguration,
                       containerName: String,
                       blobName: String?): Stream<TypedInstance> {
      val connectStr = azureStoreConnection.connectionString
      val blobServiceClient = BlobServiceClientBuilder().connectionString(connectStr).buildAsyncClient()
      val blobContainerAsyncClient = blobServiceClient.getBlobContainerAsyncClient(containerName)

      return if (blobName == null) {
        blobContainerAsyncClient.listBlobs()
            .subscribeOn(Schedulers.boundedElastic())
            .filter { !it.isDeleted }
            .flatMap { blobItem ->
               val tempFile =  File.createTempFile("azure", "vyne")
               blobContainerAsyncClient.getBlobAsyncClient(blobItem.name).downloadToFile(tempFile.absolutePath, true)
                  .subscribeOn(Schedulers.boundedElastic())
                  .map { typedInstanceStream(tempFile.inputStream(), targetType, schema, formatDetector()) }

            }.toStream()
            .flatMap { it }

      } else {

         val pipedOutputStream = PipedOutputStream()
         val pipedInputStream = PipedInputStream(pipedOutputStream)
         blobContainerAsyncClient.getBlobAsyncClient(blobName).downloadStream()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnComplete { pipedOutputStream.close() }
            .subscribe { byteBuffer -> pipedOutputStream.write(byteBuffer.array()) }

          typedInstanceStream(pipedInputStream, targetType, schema, formatDetector())
      }
   }
}

class SimpleStreamProvider(private val typedInstanceStream: Stream<TypedInstance>) : StreamProvider {
   override fun stream(targetType: Type,
                       schema: Schema,
                       azureStoreConnection: AzureStorageConnectorConfiguration,
                       containerName: String,
                       blobName: String?): Stream<TypedInstance> = typedInstanceStream
}
