package io.vyne.connectors.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import io.vyne.connectors.collectionTypeOrType
import io.vyne.connectors.config.jdbc.JdbcConnectionConfiguration
import io.vyne.connectors.jdbc.sql.dml.SelectStatementGenerator
import io.vyne.connectors.resultType
import io.vyne.models.DataSource
import io.vyne.models.OperationResult
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.query.*
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schema.api.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import io.vyne.utils.withQueryId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.query.TaxiQlQuery
import lang.taxi.services.OperationScope
import mu.KotlinLogging
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.util.LinkedCaseInsensitiveMap
import java.time.Duration
import java.time.Instant

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
