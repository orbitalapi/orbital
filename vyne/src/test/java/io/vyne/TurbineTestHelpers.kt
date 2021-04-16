package io.vyne

import app.cash.turbine.FlowTurbine
import com.winterbe.expekt.should
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject

suspend inline fun <reified O> FlowTurbine<*>.expectAs(): O {
   return expectItem() as O
}

suspend inline fun FlowTurbine<*>.expectTypedObject(): TypedObject {
   return expectItem() as TypedObject
}

suspend inline fun FlowTurbine<*>.expectTypedInstance(): TypedInstance {
   return expectItem() as TypedInstance
}

@Deprecated("Returning TypedCollection from a query is likely a bug, as we should be converting that to a flux of TypedObject")
suspend fun FlowTurbine<*>.expectTypedCollection(): TypedCollection {
   return expectItem() as TypedCollection
}

suspend inline fun FlowTurbine<*>.expectTypedObjects(count: Int): List<TypedObject> {
   val result = mutableListOf<TypedObject>()
   (0 until count).forEach { _ ->
      result.add(expectTypedObject())
   }
   return result
}

suspend inline fun FlowTurbine<*>.expectRawMap(): Map<String, Any?> {
   @Suppress("UNCHECKED_CAST")
   return expectItem() as Map<String, Any>
}

suspend inline fun FlowTurbine<*>.expectRawMapsToEqual(maps: List<Map<String, Any?>>) {
   @Suppress("UNCHECKED_CAST")
   maps.forEach {
      expectRawMap().should.equal(it)
   }
}

suspend inline fun FlowTurbine<*>.expectManyRawMaps(count: Int): List<Map<String, Any?>> {
   return expectMany<Map<String, Any>>(count)
}

suspend inline fun <reified O> FlowTurbine<*>.expectMany(count: Int): List<O> {
   val results = mutableListOf<O>()
   (0 until count).forEach { _ ->
      results.add(expectItem() as O)
   }
   return results
}



