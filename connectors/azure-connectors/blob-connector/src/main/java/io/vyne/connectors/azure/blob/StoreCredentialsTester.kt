package io.vyne.connectors.azure.blob

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.StorageAccountInfo
import io.vyne.connectors.azure.blob.registry.AzureStorageConnectorConfiguration
import reactor.core.publisher.Mono

object StoreCredentialsTester {
   fun testConnection(connectionConfig: AzureStorageConnectorConfiguration): Mono<StorageAccountInfo> {
      val connectStr = connectionConfig.connectionString
      val blobServiceClient = BlobServiceClientBuilder().connectionString(connectStr).buildAsyncClient()
      return blobServiceClient.accountInfo
   }
}
