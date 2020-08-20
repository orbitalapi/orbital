package io.vyne.pipelines.orchestrator

import com.fasterxml.classmate.TypeResolver
import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.pipelines.Pipeline
import io.vyne.pipelines.orchestrator.configuration.PipelineConfigurationProperties
import io.vyne.pipelines.orchestrator.runners.PipelineRunnerApi
import io.vyne.pipelines.runner.transport.PipelineJacksonModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackageClasses = [PipelineRunnerApi::class])
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
      fun api(resolver: TypeResolver): Docket {
         return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage(this.javaClass.`package`.name))
            .paths(PathSelectors.any())
            .build()
            .additionalModels(resolver.resolve(Pipeline::class.java))
            .directModelSubstitute(VersionedTypeReference::class.java, String::class.java)

      }

   }
}
