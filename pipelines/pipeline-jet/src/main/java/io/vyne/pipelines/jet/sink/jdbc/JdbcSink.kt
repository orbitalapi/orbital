package io.vyne.pipelines.jet.sink.jdbc

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.jdbc.sql.ddl.TableGenerator
import io.vyne.connectors.jdbc.sql.ddl.ViewGenerator
import io.vyne.connectors.jdbc.sql.dml.InsertStatementGenerator
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.MessageSourceWithGroupId
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.jdbc.WriteDisposition
import io.vyne.pipelines.jet.sink.WindowingPipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import org.jooq.DSLContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.annotation.Resource


@Component
class JdbcSinkBuilder : WindowingPipelineSinkBuilder<JdbcTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is JdbcTransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: JdbcTransportOutputSpec, schema: Schema
   ): QualifiedName {
      return pipelineTransportSpec.targetType.typeName
   }

   override fun build(
      pipelineId: String, pipelineName: String, pipelineTransportSpec: JdbcTransportOutputSpec
   ): Sink<WindowResult<List<MessageContentProvider>>> {
      fun createTable(context: JdbcSinkContext) {
         val schema = context.schema()
         val targetType = schema.type(pipelineTransportSpec.targetType)
         val (tableName, ddlStatement, indexStatements) = TableGenerator(schema).generate(
            targetType,
            context.sqlDsl(),
            context.tableNameSuffix
         )
         context.logger.info("Executing CREATE IF NOT EXISTS for table to store type ${pipelineTransportSpec.targetTypeName} as table $tableName.")

         context.logger.fine(ddlStatement.sql)
         ddlStatement.execute()
         val tableFoundAtDatabase = DatabaseMetadataService(context.jdbcTemplate()).listTables()
            .any { it.tableName.equals(tableName, ignoreCase = true) }
         if (tableFoundAtDatabase) {
            context.logger.info("${pipelineTransportSpec.targetTypeName} => Table $tableName created")

            if (indexStatements.isNotEmpty()) {
               context.logger.info("${pipelineTransportSpec.targetTypeName} => Creating indexes for $tableName")
               indexStatements.forEach { indexStatement ->
                  context.logger.info("${pipelineTransportSpec.targetTypeName} => creating index => ${indexStatement.sql}")
                  indexStatement.execute()
               }
            }
         } else {
            context.logger.severe("${pipelineTransportSpec.targetTypeName} => Failed to create database table $tableName.  No error was thrown, but the table was not found in the schema after the statement executed")
         }
      }

      return SinkBuilder.sinkBuilder("jdbc-sink") { context ->
         val sinkContext = context.managedContext().initialize(
            JdbcSinkContext(
               context.logger(), context.hazelcastInstance(), pipelineTransportSpec
            )
         ) as JdbcSinkContext

         // The table can be created in advance in case the append mode is used since the name will be constant
         if (pipelineTransportSpec.writeDisposition == WriteDisposition.APPEND) {
            createTable(sinkContext)
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
         if (pipelineTransportSpec.writeDisposition == WriteDisposition.RECREATE) {
            val sourceMessageMetadata = result.firstOrNull()?.sourceMessageMetadata
            if (sourceMessageMetadata is MessageSourceWithGroupId) {
               context.tableNameSuffix = "_${sourceMessageMetadata.groupId}"
               createTable(context)
            }
         }

         val targetType = schema.type(pipelineTransportSpec.targetType)
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
            typedInstances, context.sqlDsl(), useUpsertSemantics = true, tableNameSuffix = context.tableNameSuffix
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
            val targetType = context.schema().type(pipelineTransportSpec.targetType)
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
         if (tableNameSuffix != null) {
            error("Table name suffix has been set already. ")
         }
         field = value
      }

   @Resource
   lateinit var vyneProvider: VyneProvider

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
      return vyneProvider.createVyne().schema
   }
}

