package io.vyne.search.elastic

import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ElasticClientFactory {
   @Bean
   fun elasticClient(@Value("\${elastic.host:localhost}") elasticHost: String,
                     @Value("\${elastic.port:9200}") elasticPort: Int,
                     @Value("\${elastic.scheme:http}") elasticScheme: String

   ): RestHighLevelClient {
      return RestHighLevelClient(
         RestClient.builder(
            HttpHost(elasticHost, elasticPort, elasticScheme)))

   }
}
