package io.vyne.spring

import io.vyne.schema.consumer.http.HttpVersionedSchemaProvider
import org.springframework.context.annotation.Configuration
import reactivefeign.spring.config.EnableReactiveFeignClients


@Configuration
@EnableReactiveFeignClients(clients = [HttpVersionedSchemaProvider::class])
class HttpVersionedSchemaProviderFeignConfig {
}
