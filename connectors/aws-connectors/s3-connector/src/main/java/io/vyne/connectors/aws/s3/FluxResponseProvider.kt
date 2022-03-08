package io.vyne.connectors.aws.s3

import reactor.core.publisher.Flux
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture


class FluxResponseProvider: AsyncResponseTransformer<GetObjectResponse, FluxResponse> {
   private val response = FluxResponse()
   override fun prepare(): CompletableFuture<FluxResponse> {
      return response.cf
   }

   override fun onResponse(sdkResponse: GetObjectResponse) {
      response.sdkResponse = sdkResponse

   }

   override fun onStream(publisher: SdkPublisher<ByteBuffer>) {
      response.flux = Flux.from(publisher)
      response.cf.complete(response)
   }

   override fun exceptionOccurred(error: Throwable) {
      response.cf.completeExceptionally(error)
   }
}
data class FluxResponse(
   val cf: CompletableFuture<FluxResponse> = CompletableFuture<FluxResponse>(),
   var sdkResponse: GetObjectResponse? = null,
   var flux: Flux<ByteBuffer>? = null
)
