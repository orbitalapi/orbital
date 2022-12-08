package io.vyne

import io.vyne.models.TypedInstance
import io.vyne.models.serde.SerializableTypedInstance
import io.vyne.query.ResultMode
import io.vyne.remote.RemoteVyneQueryService
import io.vyne.schemas.Schema
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

/**
 * A service capable of executing streaming queries against a Vyne instance over an HTTP connection.
 */
@Component
class WebClientVyneQueryService(
   private val webClientBuilder: WebClient.Builder,
   private val queryServiceUrl: String = "http://query-server"
) : RemoteVyneQueryService {
   override fun <T : Any> queryWithType(query: String, type: Class<T>, schema: Schema?): Flux<T> {
      val resultMode: ResultMode = if (type == TypedInstance::class.java) ResultMode.VERBOSE else ResultMode.RAW
      val client = webClientBuilder
         .baseUrl("$queryServiceUrl/api/taxiql?resultMode=${resultMode.name}")
         .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
         .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
         .let {
            val contentType =
               if (type == TypedInstance::class.java) MediaType.APPLICATION_CBOR_VALUE else MediaType.APPLICATION_JSON_VALUE
            it.defaultHeader("ContentSerializationFormat", contentType)
         }
         .build()
      return try {
         client.post().bodyValue(query).exchangeToFlux { response ->
            if (resultMode == ResultMode.VERBOSE) {
               response.bodyToFlux(Array<Byte>::class.java).map { bytes ->
                  SerializableTypedInstance.fromBytes(bytes.toByteArray()).toTypedInstance(schema!!)
               } as Flux<T>
            } else {
               response.bodyToFlux(type)
            }
         }
      } catch (exception: Exception) {
         Flux.error(exception)
      }
   }
}
