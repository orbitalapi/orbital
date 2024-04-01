package com.orbitalhq.connectors.hazelcast.invoker

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.internal.server.tcp.TcpServerConnectionChannelErrorHandler
import com.hazelcast.map.IMap
import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mu.KotlinLogging

class HazelcastMutatingInvoker {

   companion object {
      private val logger = KotlinLogging.logger {}
   }
   fun invoke(
      hazelcastInstance: HazelcastInstance,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions,
      schema: Schema
   ): Flow<TypedInstance> {
      val (param, valueToSave) = parameters[0]
      require(valueToSave is TypedObject) { "Only TypedObjects are supported - Need to add support for ${valueToSave::class.simpleName}" }
      val (key, serializedValue) = if (valueToSave.type.hasMetadata(HazelcastTaxi.Annotations.CompactObject)) {
         GenericRecordWriter.getGenericRecordAndKey(valueToSave, schema)
      } else {
         TODO("Hazelcast persistence only supported for CompactObject currently - Support for JsonObject required")
      }
      val (keyFieldName, keyField) = findKeyField(valueToSave.type)
      val mapName = getMapName(valueToSave.type)

      logger.debug { "Setting value on map $mapName with key $key to ${serializedValue::class.simpleName}" }
      val map:IMap<Any,Any> = hazelcastInstance.getMap(mapName)
      map[key] = serializedValue
      return flowOf(valueToSave)
   }
}
