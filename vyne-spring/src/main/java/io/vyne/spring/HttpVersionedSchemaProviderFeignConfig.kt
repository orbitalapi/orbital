package io.vyne.spring

import feign.codec.Decoder
import feign.codec.Encoder
import io.vyne.schemaStore.HttpVersionedSchemaProvider
import io.vyne.schemaStore.SchemaStoreService
import org.springframework.beans.factory.ObjectFactory
import org.springframework.boot.autoconfigure.http.HttpMessageConverters
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.support.SpringDecoder
import org.springframework.cloud.openfeign.support.SpringEncoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactivefeign.spring.config.EnableReactiveFeignClients


//@Configuration
//@EnableReactiveFeignClients(clients = [HttpVersionedSchemaProvider::class])
class HttpVersionedSchemaProviderFeignConfig {
   //private val messageConverters: ObjectFactory<HttpMessageConverters> = ObjectFactory<HttpMessageConverters> { HttpMessageConverters() }

   //@Bean
   //fun feignEncoder(): Encoder? {
   //   return SpringEncoder(messageConverters)
   //}

   //@Bean
   //fun feignDecoder(): Decoder? {
   //   return SpringDecoder(messageConverters)
   //}
}
