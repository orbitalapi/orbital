package com.orbitalhq.connectors.jdbc

import com.orbitalhq.connectors.config.jdbc.JdbcConnectionConfiguration
import com.orbitalhq.connectors.jdbc.drivers.databaseSupport
import com.orbitalhq.connectors.jdbc.sql.ddl.TableGenerator
import com.orbitalhq.connectors.jdbc.sql.dml.InsertStatementGenerator
import com.orbitalhq.models.DataSourceUpdater
import com.orbitalhq.models.OperationResult
import com.orbitalhq.models.OperationResultReference
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.QueryContextEventDispatcher
import com.orbitalhq.query.RemoteCall
import com.orbitalhq.query.ResponseMessageType
import com.orbitalhq.query.SqlExchange
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.schemas.Metadata
import com.orbitalhq.schemas.OperationInvocationException
import com.orbitalhq.schemas.Parameter
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn
import com.orbitalhq.schemas.hasMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import lang.taxi.types.annotation
import mu.KotlinLogging
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

enum class UpsertVerb {
   Insert,
   Update,
   Upsert;

   companion object {
      fun forAnnotations(metadata: List<Metadata>): UpsertVerb? {
         return when {
            metadata.hasMetadata(JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName.parameterizedName) -> Upsert
            metadata.hasMetadata(JdbcConnectorTaxi.Annotations.InsertOperationAnnotationName.parameterizedName) -> Insert
            metadata.hasMetadata(JdbcConnectorTaxi.Annotations.UpdateOperationAnnotationName.parameterizedName) -> Update
            else -> null
         }
      }
   }
}

class JdbcUpsertInvoker(
   connectionFactory: JdbcConnectionFactory,
   private val schemaProvider: SchemaProvider,
) : BaseJdbcOperationInvoker(connectionFactory) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }


   /**
    * We have single instance of JdbcUpsertInvoker instantiated by singleton JdbcInvoker bean. Therefore, we need a mechanism
    * to check to see whether we need to create the underlying relational table for different data connections.
    */
   private val tableCheckAndExistsMap = ConcurrentHashMap<JdbcConnectorTaxi.Annotations.Table, JdbcConnectorTaxi.Annotations.Table>()

   override suspend fun invoke(
      service: Service,
      operation: RemoteOperation,
      parameters: List<Pair<Parameter, TypedInstance>>,
      eventDispatcher: QueryContextEventDispatcher,
      queryId: String,
      verb: UpsertVerb?
   ): Flow<TypedInstance> {
      require(verb != null)
      val schema = schemaProvider.schema

      require(operation.parameters.size == 1) { "Operations annotated with ${JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName} should accept exactly one type" }
      val inputType = operation.parameters.single().type.let { type -> type.collectionType ?: type }
      require(inputType.hasMetadata(JdbcConnectorTaxi.Annotations.Table.NAME.fqn())) { "The input type into an ${JdbcConnectorTaxi.Annotations.UpsertOperationAnnotationName} operation should be a type with a ${JdbcConnectorTaxi.Annotations.Table.NAME.fqn()} annotation" }

      val (param, input) = parameters.singleOrNull()
         ?: error("Expected a single parameter, but received ${parameters.size}")
      val tableAnnotation =
         JdbcConnectorTaxi.Annotations.Table.from(inputType.taxiType.annotation(JdbcConnectorTaxi.Annotations.Table.NAME)!!)

      val (connectionConfig, jdbcTemplate) = getConnectionConfigAndTemplate(service)
      val dsl = sqlDsl(tableAnnotation.connectionName)

      // Create underlying table if required. The entire method invocation is performed atomically, so the function is applied at most once per key.
      // Some attempted update operations on this map by other threads may be blocked while computation is in progress,
      // so the computation should be short and simple, and must not attempt to update any other mappings of this map.
      tableCheckAndExistsMap.computeIfAbsent(tableAnnotation) {
         createTableIfNotPresent(operation, tableAnnotation, dsl, jdbcTemplate, connectionConfig)
         tableAnnotation
      }

      val inputAsList = when (input) {
         is TypedCollection -> input
         is TypedObject -> listOf(input)
         else -> error("Expected either a TypedCollection or a TypedObject")
      }
      val sqlOperation = InsertStatementGenerator(
         schema,
         connectionConfig.databaseSupport
      ).generateInsertAsSingleStatement(
         values = inputAsList,
         sql = dsl,
         verb = verb,
      )
      logger.info { "Writing INSERT to table ${tableAnnotation.tableName}" }
      val startTime = Instant.now()
      try {
         val (affectedRecordCount, insertedRecords) = sqlOperation.execute()

         if (inputAsList.size == affectedRecordCount) {
            logger.info { "Successfully inserted $affectedRecordCount record(s) into table ${tableAnnotation.tableName}" }
         } else {
            logger.warn { "Was passed ${inputAsList.size} records to insert to table ${tableAnnotation.tableName}, but $affectedRecordCount were inserted" }
         }

         val remoteCall =
            buildRemoteCall(service, connectionConfig, operation, sqlOperation.sql, startTime, errorMessage = null)
         val operationResult = OperationResult.fromTypedInstances(
            parameters.map { it.second },
            remoteCall
         )
         eventDispatcher.reportRemoteOperationInvoked(
            operationResult, queryId
         )

         return if (insertedRecords != null) {
            mergeUpdatedValuesToSource(
               inputAsList,
               insertedRecords,
               inputType,
               schema,
               operationResult.asOperationReferenceDataSource()
            ).asFlow()
         } else {
            inputAsList.map { DataSourceUpdater.update(it, operationResult.asOperationReferenceDataSource()) }
               .asFlow()
         }
      } catch (e: Exception) {
         val errorMessage = "Failed to insert into ${tableAnnotation.tableName} - ${e.message}"
         logger.error(e) { errorMessage }

         val remoteCall =
            buildRemoteCall(service, connectionConfig, operation, sqlOperation.sql, startTime, errorMessage)
         eventDispatcher.reportRemoteOperationInvoked(
            OperationResult.fromTypedInstances(
               parameters.map { it.second },
               remoteCall
            ), queryId
         )

         throw OperationInvocationException(
            "Failed to insert into ${tableAnnotation.tableName}: ${e.message}",
            0,
            remoteCall, parameters
         )
      }
   }

   private fun mergeUpdatedValuesToSource(
      source: List<TypedInstance>,
      persisted: Result<Record>,
      type: Type,
      schema: Schema,
      dataSource: OperationResultReference
   ): List<TypedInstance> {
      require(source.size == persisted.size) { "Record count mismatch: Was passed ${source.size} records to write, but only ${persisted.size} were returned from the db write operation. Can't map results back to inputs." }
      return source.mapIndexed { index, typedInstance ->

         val sourceMap = (typedInstance as TypedObject).toRawObject() as Map<String, Any>
         val updated = persisted.get(index).intoMap()
         TypedInstance.from(type, sourceMap + updated, schema, source = dataSource)
      }
   }

   private fun buildRemoteCall(
      service: Service,
      connectionConfig: JdbcConnectionConfiguration,
      operation: RemoteOperation,
      sql: String,
      startTime: Instant,
      errorMessage: String?
   ) = RemoteCall(
      service = service.name,
      address = connectionConfig.address,
      operation = operation.name,
      responseTypeName = operation.returnType.name,
      requestBody = sql,
      durationMs = Duration.between(startTime, Instant.now()).toMillis(),
      timestamp = Instant.now(),
      responseMessageType = ResponseMessageType.FULL,
      response = errorMessage,
      exchange = SqlExchange(
         sql,
         0,
         "UPSERT"
      ),
      isFailed = errorMessage != null
   )

   /**
    * Ensures the table is present.
    * Currently, does not validate the table matches the schema.
    * Returns the name of the table in the schema.
    */
   private fun createTableIfNotPresent(
      operation: RemoteOperation,
      tableAnnotation: JdbcConnectorTaxi.Annotations.Table,
      dsl: DSLContext,
      namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
      connectionConfig: JdbcConnectionConfiguration
   ): String {

      val returnType = operation.returnType.collectionType ?: operation.returnType
      val (tableName, ddlStatement, indexStatements) = TableGenerator(
         schemaProvider.schema,
         connectionConfig.databaseSupport
      ).generate(
         returnType,
         dsl,
         tableNameSuffix = null,
         providedTableName = tableAnnotation.tableName
      )

      val tableAlreadyExistsAtDatabase =
         DatabaseMetadataService(namedParameterJdbcTemplate.jdbcTemplate, connectionConfig).tableExists(null, tableName)
//         .any { it.tableName.equals(tabltableExistseName, ignoreCase = true) }

      if (tableAlreadyExistsAtDatabase) {
         // TODO : Validate the table is the same
         logger.info { "Table $tableName already exists in database, so not creating." }
         return tableName
      }

      logger.info { "Executing CREATE IF NOT EXISTS for table to store type ${returnType.name.shortDisplayName} as table $tableName." }
      logger.debug { ddlStatement.sql }
      ddlStatement.execute()

      val tableFoundAtDatabase =
         DatabaseMetadataService(namedParameterJdbcTemplate.jdbcTemplate, connectionConfig).tableExists(null, tableName)
      if (tableFoundAtDatabase) {
         logger.info("${returnType.name.shortDisplayName} => Table $tableName created")

         if (indexStatements.isNotEmpty()) {
            logger.debug { "${returnType.name.shortDisplayName} => Creating indexes for $tableName" }
            indexStatements.forEach { indexStatement ->
               logger.debug { "${returnType.name.shortDisplayName} => creating index => ${indexStatement.sql}" }
               indexStatement.execute()
            }
         }
      } else {
         logger.warn { "${returnType.name.shortDisplayName} => Failed to create database table $tableName.  No error was thrown, but the table was not found in the schema after the statement executed" }
      }

      return tableName
   }
}
