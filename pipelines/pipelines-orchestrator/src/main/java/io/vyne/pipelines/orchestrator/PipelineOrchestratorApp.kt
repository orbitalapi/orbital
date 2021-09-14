package io.vyne.pipelines.orchestrator

import com.fasterxml.classmate.TypeResolver
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperties
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactivefeign.spring.config.EnableReactiveFeignClients
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

private val logger = KotlinLogging.logger {}
@SpringBootApplication
@EnableDiscoveryClient
@EnableReactiveFeignClients(basePackageClasses = [PipelineRunnerApi::class])
@EnableScheduling
@EnableSwagger2
@EnableConfigurationProperties(PipelineConfigurationProperties::class)
class PipelineOrchestratorApp {
   companion object {
      @JvmStatic
      fun main(args: Array<String>) {
         val app = SpringApplication(PipelineOrchestratorApp::class.java)
         app.run(*args)
      }

      @Bean
      fun pipelineJacksonModule() = PipelineJacksonModule()

      @Bean
      fun restTemplate(objectMapper: ObjectMapper): RestTemplate {
         val template = RestTemplate()
         template.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            execution.execute(request, body);
         })

         val messageConverter = MappingJackson2HttpMessageConverter()
         messageConverter.objectMapper = objectMapper
         template.messageConverters.removeIf { it is MappingJackson2HttpMessageConverter }
         template.messageConverters.add(messageConverter)

         return template
      }

      @Bean
      fun api(): Docket {
         return Docket(DocumentationType.SWAGGER_2)
            .enable(true)
            .select()
            .apis(RequestHandlerSelectors.basePackage(this.javaClass.`package`.name))
            .paths(PathSelectors.any())
            .build()
            .additionalModels(TypeResolver().resolve(Pipeline::class.java))
            .directModelSubstitute(VersionedTypeReference::class.java, String::class.java)
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

         logger.info { "Pipeline Orchestrator version => $version" }
      }

   }

   @Configuration
   class WebfluxConfig : WebFluxConfigurer {
      override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
         registry
            .addResourceHandler("/swagger-ui.html**")
            .addResourceLocations("classpath:/META-INF/resources/")

         registry
            .addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
      }
   }

}
