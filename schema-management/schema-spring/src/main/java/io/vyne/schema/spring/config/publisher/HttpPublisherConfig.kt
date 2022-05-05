package io.vyne.schema.spring.config.publisher

import io.vyne.schema.http.HttpKeepAliveStrategy
import io.vyne.schema.publisher.HttpPollKeepAlive
import io.vyne.schema.publisher.KeepAliveStrategyId
import io.vyne.schema.publisher.ManualRemoval
import io.vyne.schema.publisher.PublisherConfiguration
import io.vyne.schema.publisher.http.HttpSchemaPublisher
import io.vyne.schema.spring.config.publisher.HttpPublisherConfigParams.Companion.KEEP_ALIVE_STRATEGY
import io.vyne.schema.spring.config.publisher.HttpPublisherConfigParams.Companion.KEEP_ALIVE_URL
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Duration


data class HttpPublisherConfigParams(
   val keepAliveStrategy: HttpKeepAliveStrategy = HttpKeepAliveStrategy.None,
   val keepAliveUrl: String? = null,
   val keepAlivePollFrequency: Duration = Duration.ofSeconds(10),
   val publishRetryFrequency: Duration = Duration.ofSeconds(3)
) {
   companion object {
      const val HTTP_CONFIG = "${SchemaPublisherConfigProperties.PUBLISHER_CONFIG}.http"
      const val KEEP_ALIVE_URL = "$HTTP_CONFIG.keep-alive-url"
      const val RETRY_FREQUENCY = "$HTTP_CONFIG.publish-retry-frequency"
      const val KEEP_ALIVE_STRATEGY = "$HTTP_CONFIG.keep-alive-strategy"

   }
}

@ConditionalOnProperty(
   SchemaPublisherConfigProperties.PUBLISHER_METHOD,
   havingValue = "Http",
   matchIfMissing = false
)
@Import(HttpSchemaPublisher::class)
@Configuration
class HttpSchemaPublisherConfig {

   @Bean
   @ConditionalOnProperty(value = [HttpPublisherConfigParams.KEEP_ALIVE_STRATEGY], havingValue = "HttpPoll")
   fun httpKeepAliveStrategy(
      @Value("\${spring.application.name:random.uuid}") publisherId: String,
      schemaPublisherConfigProperties: SchemaPublisherConfigProperties
   ): PublisherConfiguration {

      val httpConfig = schemaPublisherConfigProperties.http
      require(
         httpConfig?.keepAliveUrl.isNullOrEmpty().not()
      ) { "$KEEP_ALIVE_URL must be set when using $KEEP_ALIVE_STRATEGY of ${KeepAliveStrategyId.HttpPoll} " }
      return PublisherConfiguration(
         publisherId,
         HttpPollKeepAlive(httpConfig!!.keepAlivePollFrequency, httpConfig.keepAliveUrl!!)
      )
   }

   @Bean
   @ConditionalOnProperty(value = [HttpPublisherConfigParams.KEEP_ALIVE_STRATEGY], havingValue = "None")
   fun manualRemovalKeepAliveStrategy(
      @Value("\${spring.application.name:random.uuid}") publisherId: String,
   ): PublisherConfiguration {
      return PublisherConfiguration(publisherId, ManualRemoval)
   }
}
