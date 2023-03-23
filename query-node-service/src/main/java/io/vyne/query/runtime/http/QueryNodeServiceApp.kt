package io.vyne.query.runtime.http

import com.fasterxml.jackson.databind.MapperFeature
import io.micrometer.core.instrument.MeterRegistry
import io.vyne.history.AnalyticsConfig
import io.vyne.models.csv.CsvFormatSpec
import io.vyne.models.format.ModelFormatSpec
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.query.runtime.core.EnableVyneQueryNode
import io.vyne.spring.EnableVyne
import io.vyne.spring.VyneSchemaConsumer
import io.vyne.spring.config.ConditionallyLoadBalancedExchangeFilterFunction
import io.vyne.spring.config.DiscoveryClientConfig
import io.vyne.spring.config.VyneSpringCacheConfiguration
import io.vyne.spring.config.VyneSpringProjectionConfiguration
import io.vyne.spring.http.auth.HttpAuthConfig
import io.vyne.utils.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction
import org.springframework.cloud.client.loadbalancer.reactive.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import java.net.InetAddress
import java.util.*

@SpringBootApplication(
   exclude = [
      DataSourceAutoConfiguration::class,
      DataSourceTransactionManagerAutoConfiguration::class,
      HibernateJpaAutoConfiguration::class
   ]
)
class QueryNodeServiceApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(QueryNodeServiceApp::class.java)
         app.setBannerMode(Banner.Mode.OFF)
         app.run(*args)
      }
   }
}

@Configuration
@EnableVyne
@VyneSchemaConsumer
@EnableVyneQueryNode
@Import(
   HttpAuthConfig::class,
   DiscoveryClientConfig::class,
   AnalyticsConfig::class
)
@EnableConfigurationProperties(
   VyneSpringCacheConfiguration::class,
   VyneSpringProjectionConfiguration::class,
)
class VyneConfig

@Configuration
class WebConfig {
   @Bean
   fun csvFormatSpec(): ModelFormatSpec = CsvFormatSpec

   @Bean
   fun vyneJacksonModule() = VyneJacksonModule()

   @Bean
   fun taxiJacksonModule() = TaxiJacksonModule()

   @Bean
   fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
      return Jackson2ObjectMapperBuilderCustomizer { builder ->
         builder.featuresToEnable(
            MapperFeature.DEFAULT_VIEW_INCLUSION
         )
      }
   }

   //   @LoadBalanced
   @Bean
   fun webClientCustomizer(
      loadBalancingFilterFunction: ReactorLoadBalancerExchangeFilterFunction,
      discoveryClient: DiscoveryClient

   ): WebClientCustomizer {
      return WebClientCustomizer { webClientBuilder ->
         webClientBuilder.filter(
            ConditionallyLoadBalancedExchangeFilterFunction.onlyKnownHosts(
               discoveryClient.services,
               loadBalancingFilterFunction
            )
         )
      }
   }


   @Bean
   fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
      val hostname = InetAddress.getLocalHost().hostName;
      return MeterRegistryCustomizer { registry: MeterRegistry ->
         registry.config().commonTags(
            "hostname", hostname
         )
      }
   }


   @Autowired
   fun logInfo(@Autowired(required = false) buildInfo: BuildProperties? = null) {
      val baseVersion = buildInfo?.get("baseVersion")
      val buildNumber = buildInfo?.get("buildNumber")
      val version = if (!baseVersion.isNullOrEmpty() && buildNumber != "0" && buildInfo.version.contains("SNAPSHOT")) {
         "$baseVersion-BETA-$buildNumber"
      } else {
         buildInfo?.version ?: "Dev version"
      }

      log().info("Vyne query server version => $version")
   }

}


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
class CustomerWebFluxConfigSupport {

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
