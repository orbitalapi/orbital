package com.orbitalhq.connectors.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.models.DataSource
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.connectors.OperationInvoker
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import lang.taxi.services.OperationScope
import mu.KotlinLogging

/**
 * An invoker capable of invoking VyneQL queries in a graph search
 * It's expected that the input to this (i.e. the invoker), is the result of a QueryBuildingEvaluator,
 * where we're receiving a TypedInstance containing the VyneQL query, along with a datasource of ConstructedQuery.
 */
class JdbcInvoker(
   connectionFactory: JdbcConnectionFactory,
   schemaProvider: SchemaProvider,
   objectMapper: ObjectMapper = Jackson.defaultObjectMapper
) :
   OperationInvoker {

   private val queryInvoker = JdbcQueryInvoker(connectionFactory, schemaProvider)
   private val upsertInvoker = JdbcUpsertInvoker(connectionFactory, schemaProvider)
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(JdbcConnectorTaxi.Annotations.DatabaseOperation.NAME)
   }

   companion object {
      private val logger = KotlinLogging.logger {}

      init {
         JdbcConnectorTaxi.registerConnectionUsage()
      }
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {
      return try {
         when {
            operation.operationType == OperationScope.READ_ONLY -> queryInvoker.invoke(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId
            )

            operation.hasMetadata(JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName) -> upsertInvoker.invoke(
               service,
               operation,
               parameters,
               eventDispatcher,
               queryId
            )

            else -> error("Unhandled JDBC Operation type: ${operation.qualifiedName.parameterizedName}")
         }
      } catch (e: Exception) {
         logger.error(e) { "Exception thrown whilst invoking Jdbc operation" }
         throw e
      }
   }


}

class DatabaseQuerySource(
   val connectionName: String,
   val query: String,
   override val failedAttempts: List<DataSource> = emptyList()
) : DataSource {
   override val name: String = "DatabaseQuery"
   override val id: String = this.hashCode().toString()
}
