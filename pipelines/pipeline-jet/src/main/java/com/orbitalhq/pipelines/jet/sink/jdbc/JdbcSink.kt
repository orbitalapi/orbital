package com.orbitalhq.pipelines.jet.sink.jdbc

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.registry.JdbcConnectionRegistry
import com.orbitalhq.connectors.jdbc.sql.ddl.TableGenerator
import com.orbitalhq.connectors.jdbc.sql.ddl.ViewGenerator
import com.orbitalhq.connectors.jdbc.sql.dml.InsertStatementGenerator
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.MessageSourceWithGroupId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.TypedInstanceContentProvider
import com.orbitalhq.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import com.orbitalhq.pipelines.jet.api.transport.jdbc.WriteDisposition
import com.orbitalhq.pipelines.jet.sink.WindowingPipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import jakarta.annotation.Resource
import mu.KotlinLogging
import org.jooq.DSLContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component


@Component
class JdbcSinkBuilder : WindowingPipelineSinkBuilder<JdbcTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is JdbcTransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: JdbcTransportOutputSpec, schema: Schema
   ): QualifiedName? {
      return pipelineTransportSpec.targetType?.typeName
   }

   override fun build(
      pipelineId: String, pipelineName: String, pipelineTransportSpec: JdbcTransportOutputSpec
   ): Sink<WindowResult<List<MessageContentProvider>>> {
      fun doCreateTable(context: JdbcSinkContext, targetType: Type) {
         val schema = context.schema()
         val (tableName, ddlStatement, indexStatements) = TableGenerator(schema).generate(
            targetType,
            context.sqlDsl(),
            context.tableNameSuffix,
            pipelineTransportSpec.tableName
         )
         context.logger.info("Executing CREATE IF NOT EXISTS for table to store type ${targetType.name.shortDisplayName} as table $tableName.")

         context.logger.fine(ddlStatement.sql)
         ddlStatement.execute()
         val tableFoundAtDatabase = DatabaseMetadataService(context.jdbcTemplate()).listTables()
            .any { it.tableName.equals(tableName, ignoreCase = true) }
         if (tableFoundAtDatabase) {
            context.logger.info("${pipelineTransportSpec.targetTypeName} => Table $tableName created")

            if (indexStatements.isNotEmpty()) {
               context.logger.fine("${pipelineTransportSpec.targetTypeName} => Creating indexes for $tableName")
               indexStatements.forEach { indexStatement ->
                  context.logger.fine("${pipelineTransportSpec.targetTypeName} => creating index => ${indexStatement.sql}")
                  indexStatement.execute()
               }
            }
         } else {
            context.logger.severe("${pipelineTransportSpec.targetTypeName} => Failed to create database table $tableName.  No error was thrown, but the table was not found in the schema after the statement executed")
         }
      }

      fun createTable(context: JdbcSinkContext, targetType: Type) {
         context.hazelcastInstance.getMap<String, Boolean>("PipelineState-$pipelineId")
            .computeIfAbsent("TableCreated") {
               doCreateTable(context, targetType)
               true
            }
      }

      return SinkBuilder.sinkBuilder("jdbc-sink") { context ->
         val sinkContext = context.managedContext().initialize(
            JdbcSinkContext(
               context.logger(), context.hazelcastInstance(), pipelineTransportSpec
            )
         ) as JdbcSinkContext

         // The table can be created in advance in case the append mode is used since the name will be constant
         if (pipelineTransportSpec.writeDisposition == WriteDisposition.APPEND && pipelineTransportSpec.targetType != null) {
            createTable(sinkContext, sinkContext.schema().type(pipelineTransportSpec.targetType!!))
         }
         sinkContext
      }.receiveFn { context: JdbcSinkContext, message: WindowResult<List<MessageContentProvider>> ->
         // Create the target table if it doesn't exist
         val schema = context.schema()
         val result = message.result()
         if (result.isEmpty()) {
            context.logger.info("No messages to write to the DB.")
            return@receiveFn
         }
         val firstRecord = result.first()
         val targetType: Type = when {
            firstRecord is TypedInstanceContentProvider -> firstRecord.content.type
            pipelineTransportSpec.targetType != null -> schema.type(pipelineTransportSpec.targetType!!)
            else -> error("Unable to determine the type of the message")
         }

         // If the targetType is null, it means we're basing the type off the result we receive upstream.
         // (eg., from a query response).
         // That means we haven't created the table yet, so need to create it now.
         if (pipelineTransportSpec.targetType == null) {
            createTable(context, targetType)
         }
         if (pipelineTransportSpec.writeDisposition == WriteDisposition.RECREATE) {
            val sourceMessageMetadata = result.firstOrNull()?.sourceMessageMetadata
            if (sourceMessageMetadata is MessageSourceWithGroupId) {
               context.tableNameSuffix = "_${sourceMessageMetadata.groupId}"
               createTable(context, targetType)
            }
         }


         val typedInstances = result.mapNotNull { messageContentProvider ->
            try {
               messageContentProvider.readAsTypedInstance(targetType, schema)
            } catch (e: Exception) {
               context.logger.severe(
                  "Error in converting message to \"${targetType.fullyQualifiedName}\", skipping insert.", e
               )
               null
            }
         }
         val insertStatements = InsertStatementGenerator(schema).generateInserts(
            typedInstances,
            context.sqlDsl(),
            useUpsertSemantics = true,
            tableNameSuffix = context.tableNameSuffix,
            tableName = pipelineTransportSpec.tableName
         )
         logger.info { "${pipelineTransportSpec.targetTypeName} => Executing INSERT batch with size: ${insertStatements.size}" }
         try {
            val insertedCount = context.sqlDsl().batch(insertStatements).execute().size
            context.logger.info("Inserted $insertedCount ${targetType.fullyQualifiedName} records into DB.")
         } catch (e: Exception) {
            context.logger.severe(
               "${pipelineTransportSpec.targetTypeName} => Failed to insert ${insertStatements.size} ${targetType.fullyQualifiedName} into DB.",
               e
            )

         }
      }.destroyFn { context: JdbcSinkContext ->
         if (pipelineTransportSpec.writeDisposition == WriteDisposition.RECREATE) {
            if (context.tableNameSuffix == null) {
               context.logger.info("Not updating the DB view for ${pipelineTransportSpec.targetTypeName} as there was no data received, and as such no table was created.")
               return@destroyFn
            }
            // TODO : This will explode if we have RECREATE with a query.
            val targetType = context.schema().type(pipelineTransportSpec.targetType!!)
            val tableNamePrefix = SqlUtils.tableNameOrTypeName(targetType.taxiType)
            val tableName = "${tableNamePrefix}${context.tableNameSuffix}"
            context.logger.info("Updating the DB view for ${pipelineTransportSpec.targetTypeName} to point to the table $tableName.")
            val viewUpdatedSuccessfully = ViewGenerator().execute(targetType, tableName, context.sqlDsl()) == 0
            if (!viewUpdatedSuccessfully) {
               context.logger.severe("Failed to update the DB view for ${pipelineTransportSpec.targetTypeName} to point to the table $tableName.")
            }
            context.logger.info("DB view for ${pipelineTransportSpec.targetTypeName} now points to the table $tableName.")
         }
      }.build()
   }
}

@SpringAware
class JdbcSinkContext(
   val logger: ILogger,
   val hazelcastInstance: HazelcastInstance,
   val outputSpec: JdbcTransportOutputSpec,
   tableNameSuffix: String? = null
) {

   var tableNameSuffix: String? = tableNameSuffix
      set(value) {
         if (tableNameSuffix != null && tableNameSuffix != value) {
            error("Table name suffix has been set already to be $tableNameSuffix. It cannot be changed to $value. ")
         }
         field = value
      }

   @Resource
   lateinit var vyneClient: VyneClientWithSchema

   @Resource
   lateinit var connectionFactory: JdbcConnectionFactory

   @Resource
   lateinit var connectionRegistry: JdbcConnectionRegistry

   fun sqlDsl(): DSLContext {
      val connectionConfig = connectionRegistry.getConnection(outputSpec.connection)
      return connectionFactory.dsl(connectionConfig)
   }

   fun jdbcTemplate(): JdbcTemplate {
      val connectionConfig = connectionRegistry.getConnection(outputSpec.connection)
      return connectionFactory.jdbcTemplate(connectionConfig).jdbcTemplate
   }

   fun schema(): Schema {
      return vyneClient.schema
   }
}

