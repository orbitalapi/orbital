package io.vyne.schemaSpring

import io.vyne.httpSchemaConsumer.HttpListSchemasService
import io.vyne.httpSchemaPublisher.HttpSchemaSubmitter
import org.springframework.context.annotation.Configuration
import reactivefeign.spring.config.EnableReactiveFeignClients

// Allows to initialize Feign client conditionally on vyne.schema.publicationMethod=REMOTE
@Configuration
@EnableReactiveFeignClients(clients = [HttpSchemaSubmitter::class, HttpListSchemasService::class])
class HttpSchemaStoreFeignConfig


@Configuration
@EnableReactiveFeignClients(clients = [HttpListSchemasService::class])
class HttpSchemaListSchemasFeignConfig

@Configuration
@EnableReactiveFeignClients(clients = [HttpSchemaSubmitter::class])
class HttpSchemaSchemaSubmitterFeignConfig
