package io.vyne.queryService.schemas.importing

import mu.KotlinLogging
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

abstract class BaseUrlLoadingSchemaConverter(
   private val webClient: WebClient = WebClient.create(),
) {

   protected fun loadSchema(url: String): Mono<String> {
      logger.info { "Attempting to load schema from $url" }
      return webClient.get().uri(url).exchange()
         .flatMap { clientResponse ->
            logger.info { "Response from $url : ${clientResponse.statusCode()}" }
            clientResponse.bodyToMono<String>()
         }
   }
}
