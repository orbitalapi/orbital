package com.orbitalhq

import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryContext
import com.orbitalhq.query.QueryResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

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
   return this.typedInstances() as List<TypedObject>
}

suspend fun QueryResult.rawObjects(): List<Map<String,Any?>> {
   return this.typedObjects().map { it.toRawObject() as Map<String,Any?> }
}

suspend fun QueryResult.expectReturnsNull():TypedNull {
   return this.typedInstances().first() as TypedNull
}
suspend fun QueryResult.firstRawObject():Map<String,Any?> {
   return this.rawObjects().first()
}
suspend fun QueryResult.firstTypedCollection(): TypedCollection {
   return return this.results?.first() as TypedCollection
}
