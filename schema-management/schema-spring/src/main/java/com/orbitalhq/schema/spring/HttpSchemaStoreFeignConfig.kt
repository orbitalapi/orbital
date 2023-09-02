package com.orbitalhq.schema.spring

import com.orbitalhq.schema.consumer.http.HttpListSchemasService
import com.orbitalhq.schema.publisher.http.HttpSchemaSubmitter
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
