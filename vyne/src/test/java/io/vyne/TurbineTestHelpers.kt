package io.vyne

import app.cash.turbine.ReceiveTurbine
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject

suspend inline fun <reified O> ReceiveTurbine<*>.expectAs(): O {
   return awaitItem() as O
}

suspend inline fun ReceiveTurbine<*>.expectTypedObject(): TypedObject {
   return awaitItem() as TypedObject
}

suspend inline fun ReceiveTurbine<*>.expectTypedInstance(): TypedInstance {
   return awaitItem() as TypedInstance
}

@Deprecated("Returning TypedCollection from a query is likely a bug, as we should be converting that to a flux of TypedObject")
suspend fun ReceiveTurbine<*>.expectTypedCollection(): TypedCollection {
   return awaitItem() as TypedCollection
}

suspend inline fun ReceiveTurbine<*>.expectTypedObjects(count: Int): List<TypedObject> {
   val result = mutableListOf<TypedObject>()
   (0 until count).forEach { _ ->
      result.add(expectTypedObject())
   }
   return result
}

suspend inline fun ReceiveTurbine<*>.expectRawMap(): Map<String, Any?> {
   @Suppress("UNCHECKED_CAST")
   return awaitItem() as Map<String, Any>
}

suspend inline fun ReceiveTurbine<*>.expectListOfRawMap(): List<Map<String, Any?>> {
   @Suppress("UNCHECKED_CAST")
   return awaitItem() as List<Map<String, Any>>
}


suspend inline fun ReceiveTurbine<*>.expectRawMapsToEqual(maps: List<Map<String, Any?>>) {
   @Suppress("UNCHECKED_CAST")
   maps.forEach {
      expectRawMap().should.equal(it)
   }
}

suspend inline fun ReceiveTurbine<*>.expectManyRawMaps(count: Int): List<Map<String, Any?>> {
   return expectMany<Map<String, Any>>(count)
}

suspend inline fun <reified O> ReceiveTurbine<*>.expectMany(count: Int): List<O> {
   val results = mutableListOf<O>()
   (0 until count).forEach { index ->
      val item = try {
         awaitItem() as O
      } catch (e: Exception) {
         throw RuntimeException("Exception when trying to expectItem with index $index", e)
      }
      results.add(item)
   }
   return results
}



