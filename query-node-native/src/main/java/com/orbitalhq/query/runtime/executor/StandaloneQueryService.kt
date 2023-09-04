package com.orbitalhq.query.runtime.executor

import com.orbitalhq.DefaultPackageMetadata
import com.orbitalhq.connectors.jdbc.registry.JdbcConnections
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.executor.serverless.ServerlessQueryExecutor
import kotlinx.serialization.ExperimentalSerializationApi
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * A REST wrapper around a standalone query executor.
 *
 * Largely useful for testing
 */
@OptIn(ExperimentalSerializationApi::class)
@RestController
class StandaloneQueryService(
   private val queryExecutor: ServerlessQueryExecutor
) {

   @PostMapping("/query")
   @RegisterReflectionForBinding(DefaultPackageMetadata::class, JdbcConnections::class)
   fun executeQuery(@RequestBody message: QueryMessageCborWrapper): Any {
      return Mono.create { sink ->
         sink.success(queryExecutor.executeQuery(message))
      }.subscribeOn(Schedulers.boundedElastic())
   }

//   @PostMapping("/query/cbor", consumes = [MediaType.APPLICATION_CBOR_VALUE])
//   fun executeQuery(@RequestBody bytes: ByteArray): Any {
//      return Mono.create { sink ->
//         val queryMessage = QueryMessage.cbor.decodeFromByteArray<QueryMessage>(bytes)
//         sink.success(queryExecutor.executeQuery(queryMessage))
//      }.subscribeOn(Schedulers.boundedElastic())
//   }

}

