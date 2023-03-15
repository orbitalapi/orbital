package io.orbital.station

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import java.util.*

/**
 * Workaround to Spring 5.3 ordering of codecs, to favour Jacckson over Kotlin
 *
 * In Spring 5.3 it appears the KotlinSerializationJsonEncoder is weighted higher
 * than Jackson2JsonEncoder.
 *
 * This means if we try to return a class that is tagged with Kotlin's @Serializable annotation,
 * Spring will use Kotlin, rather than Jackson.
 *
 * This is undesirable, and causes serialization issues.  We also have a number of custom
 * Jackon serializers written, which we want to use.
 *
 * In Spring Reactive, there's no easy way to modify ordering of Codecs.
 * So, we use this adapter to swap out the order of the codecs, pushing Jackson to the front.
 *
 * https://github.com/spring-projects/spring-framework/issues/28856
 */
@Configuration
class CustomWebFluxSerializerConfig {

   @Bean
   @Primary
   fun serverCodecConfigurerAdapter(other: ServerCodecConfigurer): ServerCodecConfigurer {
      return ReOrderingServerCodecConfigurer(other)
   }

   class ReOrderingServerCodecConfigurer(private val configurer: ServerCodecConfigurer) :
      ServerCodecConfigurer by configurer {

      override fun getWriters(): MutableList<HttpMessageWriter<*>> {
         val writers = configurer.writers
         val jacksonWriterIndex =
            configurer.writers.indexOfFirst { it is EncoderHttpMessageWriter && it.encoder is Jackson2JsonEncoder }
         val kotlinSerializationWriterIndex =
            configurer.writers.indexOfFirst { it is EncoderHttpMessageWriter && it.encoder is KotlinSerializationJsonEncoder }

         if (kotlinSerializationWriterIndex == -1 || jacksonWriterIndex == -1) {
            return writers
         }

         if (kotlinSerializationWriterIndex < jacksonWriterIndex) {
            Collections.swap(writers, jacksonWriterIndex, kotlinSerializationWriterIndex)
         }
         return writers
      }
   }
}
