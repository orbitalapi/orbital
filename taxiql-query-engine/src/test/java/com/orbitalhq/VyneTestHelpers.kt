package com.orbitalhq

import com.jayway.awaitility.Awaitility
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QueryErrorEvent
import com.orbitalhq.query.QueryResult
import io.kotest.assertions.timing.eventually
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.collections.shouldNotBeEmpty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.junit.jupiter.api.fail
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun QueryContext.findBlocking(typeName: String): List<TypedInstance> {
   return this.find(typeName)
      .results?.toList() ?: emptyList()
}

suspend fun QueryContext.findFirstBlocking(typeName: String): TypedInstance {
   return this.find(typeName)
      .results?.first() ?: error("Expected to find at least one TypedInstance, but got a null Flow<>")
}

suspend fun QueryResult.firstTypedInstace(): TypedInstance {
   return this.results?.first() ?: error("The query failed - there were no results")
}

suspend fun QueryResult.firstTypedObject(): TypedObject {
   return this.firstTypedInstace() as TypedObject
}


suspend fun QueryResult.typedInstances(): List<TypedInstance> {
   return this.results?.toList() ?: error("The query failed - there were no results")
}

suspend fun QueryResult.typedObjects(): List<TypedObject> {
   val typedInstances = this.typedInstances()
   return typedInstances  as List<TypedObject>
}

suspend fun QueryResult.firstRawValue(): Any? {
   return this.typedInstances().first().value
}

suspend fun QueryResult.rawObjects(): List<Map<String, Any?>> {
   return this.typedObjects().map {
      it.toRawObject() as Map<String, Any?>
   }
}

suspend fun QueryResult.expectReturnsNull(): TypedNull {
   return this.typedInstances().first() as TypedNull
}

suspend fun QueryResult.firstRawObject(): Map<String, Any?> {
   return this.rawObjects().firstOrNull()
      ?: error("Query didn't return any results")
}


suspend fun QueryResult.shouldEmitError(timeout: Duration = Duration.ofSeconds(5)):QueryErrorEvent {
   try {
      val error = this.firstError(timeout)
      return error
   } catch (e:Exception) {
      fail(e)
   }
}
suspend fun QueryResult.firstError(timeout: Duration = Duration.ofSeconds(5)): QueryErrorEvent {
   this.typedInstances()
   return this.errors.take(1).timeout(timeout).collectList().awaitFirst()
      .single()

}

suspend fun QueryResult.firstTypedCollection(): TypedCollection {
   return return this.results?.first() as TypedCollection
}

