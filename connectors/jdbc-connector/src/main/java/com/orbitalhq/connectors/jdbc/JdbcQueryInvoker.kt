package com.orbitalhq.connectors.jdbc

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.TaxiQlInvokerUtils
import com.orbitalhq.connectors.getTaxiQlQuery
import com.orbitalhq.connectors.jdbc.sql.dml.SelectStatementGenerator
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.StreamErrorMessage
import com.orbitalhq.query.tracing.DatabaseRequest
import com.orbitalhq.query.tracing.DatabaseResponse
import com.orbitalhq.query.tracing.SpanState
import com.orbitalhq.query.tracing.TraceEventDirection
import com.orbitalhq.query.tracing.TracingEventKind
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging

class JdbcQueryInvoker(
   connectionFactory: JdbcConnectionFactory,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper,
) : BaseJdbcOperationInvoker(connectionFactory) {

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
   ): Flow<Either<StreamErrorMessage, TypedInstance>> {
      val (connectionConfig, jdbcTemplate) = getConnectionConfigAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters.getTaxiQlQuery()
      val query = TaxiQlInvokerUtils.queryOrErrorFlow(schema, taxiQuery)
         .getOrElse { errorFlow -> return errorFlow }
//      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = SelectStatementGenerator(taxiSchema).toSql(query, connectionConfig.sqlBuilder())
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }

      logger.debug { "$queryId: Starting JDBC Query $sql" }
      val stopwatch = Stopwatch.createStarted()
      val span = eventDispatcher.createOperationTraceSpan(service, operation, "")
      span.emitEvent(
         TracingEventKind.OK,
         SpanState.ACTIVE,
         operation.returnType,
         DatabaseRequest(connectionConfig.connectionName, "Select", "") { sql },
         "Select",
         direction = TraceEventDirection.OUTBOUND
      )
      val resultList = jdbcTemplate.queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      val resultEvent = span.emitEvent(
         TracingEventKind.OK,
         SpanState.COMPLETE,
         operation.returnType,
         DatabaseResponse(resultList.size.toLong()) {
            // Don't use Jackson to JSON this for aestetic reasons, as it
            // creates problem with serializers for db-specific types (like PGObjects / PGArray, etc)
            resultList.toString()
         },
         "Select",
         direction = TraceEventDirection.INBOUND
      )

      logger.debug { "$queryId: JDBC Query completed in $elapsed" }
      val operationResult = buildOperationResult(
         service,
         operation,
         constructedQueryDataSource.inputs,
         sql,
         connectionConfig.address,
         elapsed,
         recordCount = resultList.size
      )
      eventDispatcher.reportRemoteOperationInvoked(operationResult, queryId)
      return convertToTypedInstances(
         resultList,
         query,
         schema,
         operationResult.asOperationReferenceDataSource(resultEvent.idSet)
      )
   }

}
