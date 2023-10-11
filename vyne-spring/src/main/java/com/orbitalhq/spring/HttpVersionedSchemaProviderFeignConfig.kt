package com.orbitalhq.spring

import com.orbitalhq.schema.consumer.http.HttpVersionedSchemaProvider
import org.springframework.context.annotation.Configuration
import reactivefeign.spring.config.EnableReactiveFeignClients


@Configuration
@EnableReactiveFeignClients(clients = [HttpVersionedSchemaProvider::class])
class HttpVersionedSchemaProviderFeignConfig {
}
