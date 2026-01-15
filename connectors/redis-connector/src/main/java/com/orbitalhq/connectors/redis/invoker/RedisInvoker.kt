package com.orbitalhq.connectors.redis.invoker

import arrow.core.Either
import com.orbitalhq.connectors.redis.RedisConnectionProvider
import com.orbitalhq.connectors.redis.RedisTaxi
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.QueryContextSchemaProvider
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schemas.*
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope

class RedisInvoker(
   private val redisConnectionProvider: RedisConnectionProvider
) : OperationInvoker {

   private val mutatingInvoker = RedisMutatingInvoker()
   private val queryInvoker = RedisQueryInvoker()
   private val streamInvoker = RedisStreamInvoker()

   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(RedisTaxi.Annotations.RedisServiceAnnotation)
   }

   override fun plan(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RemoteCall {
      val connectionName = getRedisConnectionName(service)
      val (connection, config) = getRedisConnection(connectionName)

      return when {
         operation.operationType == OperationScope.READ_ONLY && operation.operationKind != OperationKind.Stream -> {
            queryInvoker.plan(
               connection,
               config,
               service,
               operation,
               parameters,
               schema
            )
         }

         operation.operationType == OperationScope.READ_ONLY && operation.operationKind == OperationKind.Stream -> {
            streamInvoker.plan(
               connection,
               config,
               service,
               operation,
               parameters,
               schema
            )
         }

         operation.operationType == OperationScope.MUTATION -> {
            mutatingInvoker.plan(
               connection,
               config,
               service,
               operation,
               parameters,
               schema
            )
         }

         else -> {
            error("No invoker strategy found for operation ${operation.qualifiedName.longDisplayName}")
         }
      }
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      queryOptions: QueryOptions
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val connectionName = getRedisConnectionName(service)
      val (connection, config) = getRedisConnection(connectionName)
      val schema = (eventDispatcher as QueryContextSchemaProvider).schema

      return when {
         operation.operationType == OperationScope.READ_ONLY && operation.operationKind != OperationKind.Stream -> {
            queryInvoker.invoke(
               connection,
               config,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               schema
            )
         }

         operation.operationType == OperationScope.READ_ONLY && operation.operationKind == OperationKind.Stream -> {
            streamInvoker.invoke(
               connection,
               config,
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId,
               queryOptions,
               schema
            )
         }

         operation.operationType == OperationScope.MUTATION -> {
            mutatingInvoker.invoke(
               connection,
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

   private fun getRedisConnectionName(service: Service): String? {
      val metadata = service.getMetadata(RedisTaxi.Annotations.RedisServiceAnnotation)
      return metadata.params["connectionName"] as String?
   }

   private fun getRedisConnection(connectionName: String?) =
      redisConnectionProvider.redisConnection(connectionName)
}
