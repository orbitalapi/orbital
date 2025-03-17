package com.orbitalhq.connectors.hazelcast.invoker

import arrow.core.Either
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import com.orbitalhq.connectors.config.hazelcast.HazelcastConfiguration
import com.orbitalhq.connectors.hazelcast.HazelcastTaxi
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedNull
import com.orbitalhq.models.TypedObject
import com.orbitalhq.models.json.right
import com.orbitalhq.query.CacheExchange
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.QueryOptions
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import lang.taxi.types.PrimitiveType
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

class HazelcastMutatingInvoker {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun plan(
      hazelcastInstance: HazelcastInstance,
      config: HazelcastConfiguration,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema
   ): RemoteCall {
      val (_, valueToSave) = parameters[0]
      val mapName = getMapName(valueToSave.type)

      return buildRemoteCall(
         mapName,
         service,
         config.addresses.joinToString(),
         operation,
         // We could get this now, with further refactoring
         "Available at execution time",
         Duration.ZERO,
         -1,
         getVerb(operation),
         config.connectionName
      )
   }

   private fun doUpsert(
      hazelcastInstance: HazelcastInstance,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      reportResult: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val (_, valueToSave) = parameters[0]
      val mapName = getMapName(valueToSave.type)
      require(valueToSave is TypedObject) { "Only TypedObjects are supported - Need to add support for ${valueToSave::class.simpleName}" }
      val (key, serializedValue) = if (valueToSave.type.hasMetadata(HazelcastTaxi.Annotations.CompactObject)) {
         GenericRecordWriter.getGenericRecordAndKey(valueToSave, schema)
      } else {
         HazelcastJsonValueWriter.getJsonValueAndKey(valueToSave, schema)
      }


      logger.debug { "Setting value on map $mapName with key $key to ${serializedValue::class.simpleName}" }
      val map: IMap<Any, Any> = hazelcastInstance.getMap(mapName)
      map[key] = serializedValue
      val dataSource = reportResult("UPDATE * where key = $key", mapName, 1, CacheExchange.CacheOperationVerb.UPDATE)
      val updatedValue = DataSourceUpdater.update(valueToSave, dataSource)
      return flowOf(updatedValue.right())
   }

   private fun doDelete(
      hazelcastInstance: HazelcastInstance,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      operation: RemoteOperation,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val deleteAnnotation = operation.firstMetadata(HazelcastTaxi.Annotations.DeleteOperation.parameterizedName)
      val mapName = deleteAnnotation.params["mapName"] as String?
         ?: error("Operation ${operation.qualifiedName.parameterizedName} does not declare a mapName")

      val map = hazelcastInstance.getMap<Any, Any>(mapName)
      val deleteKey = parameters.singleOrNull()?.second
      return if (deleteKey == null) {
         deleteAll(mapName, map, reportAndGenerateDataSource, schema)
      } else {
         deleteByKey(deleteKey, mapName, map, reportAndGenerateDataSource, schema, operation)
      }
   }

   private fun deleteByKey(
      deleteKey: TypedInstance,
      mapName: String,
      map: IMap<Any, Any>,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      schema: Schema,
      operation: RemoteOperation
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {

      val keyValue = deleteKey.toRawObject() ?: error("Cannot delete from map $mapName as provided key was null")
      val removedValue = map.remove(keyValue)
      val recordCount = if (removedValue != null) 1 else 0
      val dataSource = reportAndGenerateDataSource(
         "DELETE * where KEY = $deleteKey",
         mapName,
         recordCount,
         CacheExchange.CacheOperationVerb.DELETE,
      )
      val result = TypedInstance.tryFrom(operation.returnType, removedValue, schema, source = dataSource)
      return flowOf(result)
   }

   private fun deleteAll(
      mapName: String,
      map: IMap<Any, Any>,
      reportAndGenerateDataSource: (String, String, Int, CacheExchange.CacheOperationVerb) -> DataSource,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      logger.info { "Performing deleteAll on map $mapName" }
      val sizeBeforeDelete = map.size
      map.clear()
      val dataSource =
         reportAndGenerateDataSource("DELETE *", mapName, sizeBeforeDelete, CacheExchange.CacheOperationVerb.DELETE)
      // Not really sure on what we should be returning here.
      return flowOf(TypedNull.create(schema.type(PrimitiveType.VOID), source = dataSource).right())
   }

   fun invoke(
      hazelcastInstance: HazelcastInstance,
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      hazelcastConnectionConfig: HazelcastConfiguration,
      eventDispatcher: QueryContextEventDispatcher,

      queryId: String,
      queryOptions: QueryOptions,
      schema: Schema
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val startTime = Instant.now()
      fun reportResult(
         sql: String,
         mapName: String,
         resultSize: Int,
         verb: CacheExchange.CacheOperationVerb
      ): DataSource {
         val result = buildOperationResult(
            service,
            operation,
            mapName,
            parameters.map { it.second },
            hazelcastConnectionConfig,
            sql,
            Duration.between(startTime, Instant.now()),
            resultSize,
            verb
         )
         eventDispatcher.reportRemoteOperationInvoked(result, queryId)
         return result.asOperationReferenceDataSource()
      }

      return when (getVerb(operation)) {
         CacheExchange.CacheOperationVerb.UPDATE -> doUpsert(
            hazelcastInstance,
            parameters,
            schema,
            ::reportResult
         )

         CacheExchange.CacheOperationVerb.DELETE -> doDelete(
            hazelcastInstance,
            parameters,
            schema,
            operation,
            ::reportResult
         )

         else -> error("Unexpected type of mutation for Hazelcast: ${operation.qualifiedName.parameterizedName} ")
      }
   }

   private fun getVerb(operation: RemoteOperation): CacheExchange.CacheOperationVerb {
      return when {
         operation.hasMetadata(HazelcastTaxi.Annotations.UpsertOperation.parameterizedName) -> CacheExchange.CacheOperationVerb.UPDATE
         operation.hasMetadata(HazelcastTaxi.Annotations.DeleteOperation.parameterizedName) -> CacheExchange.CacheOperationVerb.DELETE
         else -> error("Unexpected type of mutation for Hazelcast: ${operation.qualifiedName.parameterizedName} ")
      }
   }


   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      mapName: String,
      parameters: List<TypedInstance>,
      connectionConfig: HazelcastConfiguration,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheExchange.CacheOperationVerb
   ): OperationResult {
      val remoteCall =
         buildRemoteCall(
            mapName,
            service,
            connectionConfig.addresses.joinToString(),
            operation,
            sql,
            elapsed,
            recordCount,
            verb,
            connectionConfig.connectionName
         )
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   /**
    * data class CacheExchange(
    *    val connectionName: String,
    *    val cacheName: String,
    *    /**
    *     * When a simple key lookup, provide the key.
    *     * If querying, provide the query statement
    *     */
    *    val cacheKeyOrStatement: String,
    *    val verb: CacheOperationVerb,
    *    val cacheType: CacheType,
    *    val recordCount: Int,
    * )
    */

   private fun buildRemoteCall(
      mapName: String,
      service: Service,
      hazelcastAddresses: String,
      operation: RemoteOperation,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: CacheExchange.CacheOperationVerb,
      connectionName: String
   ) = RemoteCall(
      service = service.name,
      address = hazelcastAddresses,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = sql,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      // If we implement streaming database queries, this will change
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = CacheExchange(
         connectionName = connectionName,
         cacheName = mapName,
         cacheKeyOrStatement = sql,
         cacheType = CacheExchange.CacheType.Hazelcast,
         recordCount = recordCount,
         verb = verb
      ),

      )
}
