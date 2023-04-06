package io.vyne.query.runtime.http

import io.vyne.DefaultPackageMetadata
import io.vyne.connectors.jdbc.registry.JdbcConnections
import io.vyne.query.runtime.QueryMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class QueryConsumer(
   private val vyneFactory: StandaloneVyneFactory
) {

   @PostMapping("/query")
   @RegisterReflectionForBinding(DefaultPackageMetadata::class, JdbcConnections::class)
   fun executeQuery(@RequestBody message: QueryMessage): Flow<Any?> {
      val vyne = vyneFactory.buildVyne(message)
      // TODO : Remove runBlocking...
      return runBlocking {
         vyne.query(message.query, clientQueryId = message.clientQueryId)
            .rawResults
      }

   }

}
