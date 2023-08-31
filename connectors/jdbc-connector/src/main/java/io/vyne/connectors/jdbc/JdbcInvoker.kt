package io.vyne.connectors.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Service
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
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String
   ): Flow<TypedInstance> {
      return when {
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
