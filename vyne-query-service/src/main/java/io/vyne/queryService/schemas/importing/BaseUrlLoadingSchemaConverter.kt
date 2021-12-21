package io.vyne.queryService.schemas.importing

import mu.KotlinLogging
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

private val logger = KotlinLogging.logger {}

abstract class BaseUrlLoadingSchemaConverter(
   private val webClient: WebClient = WebClient.create(),
   private val httpClientTimeout: Duration = Duration.ofSeconds(10)
) {

   protected fun loadSchema(url: String): String {
      logger.info { "Attempting to load swagger from $url" }
      val clientResponse = webClient.get().uri(url).exchange()
         .flatMap { clientResponse ->
            logger.info { "Response from $url : ${clientResponse.statusCode()}" }
            clientResponse.bodyToMono<String>()
         }
         .block(httpClientTimeout) ?: error("Failed to load schema from $url - timeout")
      return clientResponse
   }
}
