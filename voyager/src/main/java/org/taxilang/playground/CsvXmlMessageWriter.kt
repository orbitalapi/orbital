package org.taxilang.playground

import org.reactivestreams.Publisher
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.codec.HttpMessageWriter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * In Playground, we support returning content as CSV or XML as well.
 * However, the serializer from Spring Boot doesn't play nicely with
 * a return signature of Publisher<Any> and a custom media type.
 * so, we provide one here.
 *
 */
class CsvXmlMessageWriter : HttpMessageWriter<Any> {
   val supportedMediaTypes = mutableListOf(MediaType.parseMediaType("text/csv"), MediaType.APPLICATION_XML)

   override fun getWritableMediaTypes(): MutableList<MediaType> {
      return supportedMediaTypes
   }

   override fun canWrite(elementType: ResolvableType, mediaType: MediaType?): Boolean {
      if (mediaType == null) return false
      return supportedMediaTypes.any {  it.isCompatibleWith(mediaType) }
   }

   override fun write(
      inputStream: Publisher<out Any>,
      elementType: ResolvableType,
      mediaType: MediaType?,
      message: ReactiveHttpOutputMessage,
      hints: MutableMap<String, Any>
   ): Mono<Void> {
      return Flux.from(inputStream)
         .collectList()
         .flatMap { lines ->
            val seperator = when  {
               mediaType?.isCompatibleWith(MediaType.APPLICATION_XML) ?: false -> "\n"
               else -> ""  // no new line required, CSV already provides one
            }
            val content = lines
               .joinToString(seperator)
            val bufferFactory = message.bufferFactory()
            val buffer = bufferFactory.wrap(content.toByteArray())
            message.writeWith(Mono.just(buffer))
         }
   }
}
