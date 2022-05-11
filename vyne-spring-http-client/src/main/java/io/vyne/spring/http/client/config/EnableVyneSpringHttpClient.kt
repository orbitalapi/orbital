package io.vyne.spring.http.client.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(VyneSpringHttpClientConfiguration::class)
@EnableConfigurationProperties(VyneHttpClientConfig::class)
annotation class EnableVyneSpringHttpClient

@ConstructorBinding
@ConfigurationProperties(prefix = "vyne.http.client")
data class VyneHttpClientConfig(
   val id: String? = null,
   val secret: String? = null,
   val tokenUri: String? = null,
   val vyneUrl: String = "http://localhost:9022"
) {
   val isSecure: Boolean = (id != null && secret != null && tokenUri != null)
}
