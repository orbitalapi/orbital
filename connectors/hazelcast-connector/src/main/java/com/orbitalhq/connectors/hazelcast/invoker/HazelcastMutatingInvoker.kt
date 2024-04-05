package com.orbitalhq.connectors.hazelcast.invoker

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
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
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

   private fun doUpsert(
      hazelcastInstance: HazelcastInstance,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      reportResult: (String, Int, String) -> DataSource
   ): Flow<TypedInstance> {
      val (param, valueToSave) = parameters[0]
      require(valueToSave is TypedObject) { "Only TypedObjects are supported - Need to add support for ${valueToSave::class.simpleName}" }
      val (key, serializedValue) = if (valueToSave.type.hasMetadata(HazelcastTaxi.Annotations.CompactObject)) {
         GenericRecordWriter.getGenericRecordAndKey(valueToSave, schema)
      } else {
         TODO("Hazelcast persistence only supported for CompactObject currently - Support for JsonObject required")
      }
      val mapName = getMapName(valueToSave.type)

      logger.debug { "Setting value on map $mapName with key $key to ${serializedValue::class.simpleName}" }
      val map: IMap<Any, Any> = hazelcastInstance.getMap(mapName)
      map[key] = serializedValue
      val dataSource = reportResult("UPDATE * where key = $key", 1, "UPDATE")
      val updatedValue = DataSourceUpdater.update(valueToSave, dataSource)
      return flowOf(updatedValue)
   }

   private fun doDelete(
      hazelcastInstance: HazelcastInstance,
      parameters: List<Pair<Parameter, TypedInstance>>,
      schema: Schema,
      operation: RemoteOperation,
      reportAndGenerateDataSource: (String, Int, String) -> DataSource
   ): Flow<TypedInstance> {
      val deleteAnnotation = operation.firstMetadata(HazelcastTaxi.Annotations.DeleteOperation.parameterizedName)
      val mapName = deleteAnnotation.params.get("mapName") as String?
         ?: error("Operation ${operation.qualifiedName.parameterizedName} does not declare a mapName")

      val map = hazelcastInstance.getMap<Any, Any>(mapName)
      val deleteKey = parameters.singleOrNull()?.second
      return if (deleteKey == null) {
         deleteAll(mapName, map, reportAndGenerateDataSource, schema)
      } else {
         deleteByKey(deleteKey, mapName, map, reportAndGenerateDataSource, schema, operation)
      }
      TODO("Not yet implemented")
   }

   private fun deleteByKey(
      deleteKey: TypedInstance,
      mapName: String,
      map: IMap<Any, Any>,
      reportAndGenerateDataSource: (String, Int, String) -> DataSource,
      schema: Schema,
      operation: RemoteOperation
   ): Flow<TypedInstance> {

      val keyValue = deleteKey.toRawObject() ?: error("Cannot delete from map $mapName as provided key was null")
      val removedValue = map.remove(keyValue)
      val recordCount = if (removedValue != null) 1 else 0
      val dataSource = reportAndGenerateDataSource(
         "DELETE * where KEY = $deleteKey",
         recordCount,
         "DELETE",
      )
      val result = TypedInstance.from(operation.returnType, removedValue, schema, source = dataSource)
      return flowOf(result)
   }

   private fun deleteAll(
      mapName: String,
      map: IMap<Any, Any>,
      reportAndGenerateDataSource: (String, Int, String) -> DataSource,
      schema: Schema
   ): Flow<TypedNull> {
      logger.info { "Performing deleteAll on map $mapName" }
      val sizeBeforeDelete = map.size
      map.clear()
      val dataSource = reportAndGenerateDataSource("DELETE *", sizeBeforeDelete, "DELETE")
      // Not really sure on what we should be returning here.
      return flowOf(TypedNull.create(schema.type(PrimitiveType.VOID), source = dataSource))
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
   ): Flow<TypedInstance> {
      val startTime = Instant.now()
      fun reportResult(sql: String, resultSize: Int, verb: String): DataSource {
         val result = buildOperationResult(
            service,
            operation,
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

      return when {
         operation.hasMetadata(HazelcastTaxi.Annotations.UpsertOperation.parameterizedName) -> doUpsert(
            hazelcastInstance,
            parameters,
            schema,
            ::reportResult
         )

         operation.hasMetadata(HazelcastTaxi.Annotations.DeleteOperation.parameterizedName) -> doDelete(
            hazelcastInstance,
            parameters,
            schema,
            operation,
            ::reportResult
         )

         else -> error("Unexpected type of mutation for Hazelcast: ${operation.qualifiedName.parameterizedName} ")
      }
   }


   private fun buildOperationResult(
      service: Service,
      operation: RemoteOperation,
      parameters: List<TypedInstance>,
      connectionConfig: HazelcastConfiguration,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String = "SELECT"
   ): OperationResult {
      val remoteCall =
         buildRemoteCall(service, connectionConfig.addresses.joinToString(), operation, sql, elapsed, recordCount, verb)
      return OperationResult.fromTypedInstances(
         parameters,
         remoteCall
      )
   }

   private fun buildRemoteCall(
      service: Service,
      jdbcUrl: String,
      operation: RemoteOperation,
      sql: String,
      elapsed: Duration,
      recordCount: Int,
      verb: String
   ) = RemoteCall(
      service = service.name,
      address = jdbcUrl,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = sql,
      durationMs = elapsed.toMillis(),
      timestamp = Instant.now(),
      // If we implement streaming database queries, this will change
      responseMessageType = ResponseMessageType.FULL,
      // Feels like capturing the results are a bad idea.  Can revisit if there's a use-case
      response = null,
      exchange = SqlExchange(
         sql = sql,
         recordCount = recordCount,
         verb = verb
      ),

      )
}
