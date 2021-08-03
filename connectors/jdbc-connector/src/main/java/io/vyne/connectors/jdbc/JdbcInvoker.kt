package io.vyne.connectors.jdbc

import io.vyne.models.DataSource
import io.vyne.models.TypedInstance
import io.vyne.query.QueryContextEventDispatcher
import io.vyne.query.connectors.OperationInvoker
import io.vyne.schemaStore.SchemaProvider
import io.vyne.schemas.Parameter
import io.vyne.schemas.RemoteOperation
import io.vyne.schemas.Schema
import io.vyne.schemas.Service
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.Compiler
import lang.taxi.query.TaxiQlQuery
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class JdbcInvoker(private val connectionRegistry: JdbcConnectionRegistry, private val schemaProvider: SchemaProvider) :
   OperationInvoker {
   override fun canSupport(service: Service, operation: RemoteOperation): Boolean {
      return service.hasMetadata(Taxi.Annotations.DatabaseOperation)
   }

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String?
   ): Flow<TypedInstance> {
      val (connectionName, jdbcTemplate) = getConnectionNameAndTemplate(service)
      val schema = schemaProvider.schema()
      val taxiSchema = schema.taxi
      val taxiQuery = parameters[0].second.value as String
      val query = Compiler(taxiQuery, importSources = listOf(taxiSchema)).queries().first()
      val (sql, paramList) = TaxiQlToSqlConverter(taxiSchema).toSql(query)
      val paramMap = paramList.associate { param -> param.nameUsedInTemplate to param.value }

      val resultList = jdbcTemplate.queryForList(sql, paramMap)
      val datasource = DatabaseQuerySource(connectionName, sql)
      return convertToTypedInstances(resultList, query, schema, datasource)
   }

   private fun convertToTypedInstances(
      resultList: List<MutableMap<String, Any>>,
      query: TaxiQlQuery,
      schema: Schema,
      datasource: DatabaseQuerySource
   ): Flow<TypedInstance> {
      val resultTypeName = when {
         query.projectedType != null -> {
            query.projectedType!!.anonymousTypeDefinition?.toQualifiedName()
               ?: query.projectedType!!.concreteType?.toQualifiedName()
               ?: error("Projected type should contain either an anonymous type or a concrete type")
         }
         else -> {
            if (query.typesToFind.size > 1) {
               error("Multiple query types are not yet supported")
            } else {
               query.typesToFind.first().type
            }
         }
      }

      val resultTaxiType = collectionTypeOrType(schema.taxi.type(resultTypeName))

      val typedInstances = resultList
         .map { columnMap ->
            TypedInstance.from(
               schema.type(resultTaxiType),
               columnMap,
               schema,
               source = datasource
            )
         }
      return typedInstances.asFlow()
   }

   private fun getConnectionNameAndTemplate(service: Service): Pair<String, NamedParameterJdbcTemplate> {
      val connectionName = service.metadata(Taxi.Annotations.DatabaseOperation).params["connectionName"] as String
      val connection = connectionRegistry.getConnection(connectionName)
      return connectionName to connection.build()
   }
}

class DatabaseQuerySource(val connectionName: String, val query: String, override val failedAttempts: List<DataSource> = emptyList()) : DataSource {
   override val name: String = "DatabaseQuery"
   override val id: String = this.hashCode().toString()
}
