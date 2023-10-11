package com.orbitalhq.connectors.jdbc

import com.google.common.base.Stopwatch
import com.orbitalhq.connectors.jdbc.sql.dml.SelectStatementGenerator
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.ConstructedQueryDataSource
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Service
import com.orbitalhq.utils.withQueryId
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging

class JdbcQueryInvoker(
   connectionFactory: JdbcConnectionFactory,
   private val schemaProvider: SchemaProvider,
) : BaseJdbcOperationInvoker(connectionFactory, ) {

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
      val (connectionConfig, jdbcTemplate) = getConnectionConfigAndTemplate(service)
      val schema = schemaProvider.schema
      val taxiSchema = schema.taxi
      val (taxiQuery, constructedQueryDataSource) = parameters[0].second.let { it.value as String to it.source as ConstructedQueryDataSource }
      val (query, _) = schema.parseQuery(taxiQuery)
//      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = SelectStatementGenerator(taxiSchema).toSql(query, connectionConfig.sqlBuilder())
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }

      logger.withQueryId(queryId).debug { "Starting JDBC Query $sql" }
      val stopwatch = Stopwatch.createStarted()
      val resultList = jdbcTemplate.queryForList(sql, paramMap)
      val elapsed = stopwatch.elapsed()
      logger.withQueryId(queryId).debug { "JDBC Query completed in $elapsed" }
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
      return convertToTypedInstances(resultList, query, schema, operationResult.asOperationReferenceDataSource())
   }

}
