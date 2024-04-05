package com.orbitalhq.connectors.hazelcast.invoker

import com.orbitalhq.connectors.hazelcast.HazelcastInstanceProvider
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.AttributeName
import com.orbitalhq.schemas.Field
import com.orbitalhq.schemas.OperationKind
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.fqn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope
import kotlin.coroutines.coroutineContext

class HazelcastInvoker(
   private val hazelcastInstanceProvider: HazelcastInstanceProvider
) : OperationInvoker {
   private val mutatingInvoker = HazelcastMutatingInvoker()
   private val queryInvoker = HazelcastQueryInvoker()
   private val streamInvoker = HazelcastStreamInvoker()
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(HazelcastTaxi.Annotations.HazelcastServiceAnnotation)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<TypedInstance> {
      val connectionName = getHazelcastConnectionName(service)
      val (hazelcastInstance, config) = getHazelcastConnection(connectionName)
      val schema = (eventDispatcher as QueryContextSchemaProvider).schema
      return when {
         operation.operationType == OperationScope.READ_ONLY && operation.operationKind != OperationKind.Stream -> {
            queryInvoker.invoke(
               hazelcastInstance,
               config,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               schema,
            )
         }

         operation.operationType == OperationScope.READ_ONLY && operation.operationKind == OperationKind.Stream -> {
            streamInvoker.invoke(
               hazelcastInstance,
               config,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               schema,
            )

         }


         operation.operationType == OperationScope.MUTATION -> {
            mutatingInvoker.invoke(
               hazelcastInstance,
               service,
               operation,
               parameters,
               config,
               eventDispatcher,
               queryId,
               queryOptions,
               schema
            )
         }

         else -> {
            error("No invoker strategy found for operation ${operation.qualifiedName.longDisplayName}")
         }
      }
   }

   private fun getHazelcastConnection(connectionName: String) =
      hazelcastInstanceProvider.hazelcastConnection(connectionName)

   private fun getHazelcastConnectionName(service: Service): String {
      require(service.hasMetadata(HazelcastTaxi.Annotations.HazelcastServiceAnnotation)) { "Service ${service.qualifiedName.longDisplayName} does not declare a @HazelcastService annotation" }
      val annotation = service.firstMetadata(HazelcastTaxi.Annotations.HazelcastServiceAnnotation)
      val connectionName = annotation.params["connectionName"] as String?
         ?: error("No connectionName defined on HazelcastService annotation")
      return connectionName
   }
}

fun findKeyField(type: com.orbitalhq.schemas.Type): Pair<AttributeName, Field> {
   val keyField = type.getAttributesWithAnnotation("Id".fqn())
   return when (keyField.size) {
      1 -> keyField.entries.single().let { (k, v) -> k to v }
      else -> error("Cannot persist type ${type.qualifiedName.longDisplayName} to a Hazelcast map, as there are ${keyField.size} fields with an @Id annotated - expected exactly one.")
   }
}

fun getMapName(type: com.orbitalhq.schemas.Type): String {
   if (!type.hasMetadata(HazelcastTaxi.Annotations.HazelcastMap)) {
      error("Cannot persist type ${type.qualifiedName.longDisplayName} to a Hazelcast map, as it does not have a @${HazelcastTaxi.Annotations.HazelcastMap.longDisplayName} annotation.")
   }
   val metadata = type.getMetadata(HazelcastTaxi.Annotations.HazelcastMap)
   return metadata.params["name"] as String
}
