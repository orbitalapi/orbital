package io.vyne.pipelines.jet.sink.jdbc

import com.hazelcast.jet.datamodel.WindowResult
import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.connectors.jdbc.sql.ddl.TableGenerator
import io.vyne.connectors.jdbc.sql.dml.InsertStatementGenerator
import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
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
class JdbcSinkBuilder :
   WindowingPipelineSinkBuilder<JdbcTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is JdbcTransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: JdbcTransportOutputSpec,
      schema: Schema
   ): QualifiedName {
      return pipelineTransportSpec.targetType.typeName
   }

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: JdbcTransportOutputSpec
   ): Sink<WindowResult<List<MessageContentProvider>>> {
      return SinkBuilder
         .sinkBuilder("jdbc-sink") { context ->
            val sinkContext = context.managedContext().initialize(
               JdbcSinkContext(
                  context.logger(),
                  pipelineTransportSpec
               )
            ) as JdbcSinkContext

            // Create the target table if it doesn't exist
            val schema = sinkContext.schema()
            val (tableName, ddlStatement, indexStatements) = TableGenerator(schema)
               .generate(schema.type(pipelineTransportSpec.targetType), sinkContext.sqlDsl())
            context.logger()
               .info("Executing CREATE IF NOT EXISTS for table to store type ${pipelineTransportSpec.targetTypeName} as table $tableName")

            context.logger().fine(ddlStatement.sql)
            ddlStatement.execute()
            val tableFoundAtDatabase = DatabaseMetadataService(sinkContext.jdbcTemplate())
               .listTables()
               .any { it.tableName.equals(tableName, ignoreCase = true) }
            if (tableFoundAtDatabase) {
               context.logger()
                  .info("Table $tableName created")

               if (indexStatements.isNotEmpty()) {
                  context.logger().info("Creating indexes for $tableName")
                  indexStatements.forEach { indexStatement ->
                     context.logger().info("creating index => ${indexStatement.sql}")
                     indexStatement.execute()
                  }
               }
            } else {
               context.logger()
                  .severe("Failed to create database table $tableName.  No error was thrown, but the table was not found in the schema after the statement executed")
            }

            sinkContext
         }
         .receiveFn { context: JdbcSinkContext, message: WindowResult<List<MessageContentProvider>> ->
            val schema = context.schema()
            val targetType = schema.type(pipelineTransportSpec.targetType)
            val typedInstances = message.result().mapNotNull { messageContentProvider ->
               try {
                  messageContentProvider.readAsTypedInstance(ConsoleLogger, targetType, schema)
                  //TypedInstance.from(targetType, messageContentProvider.asString(ConsoleLogger), schema)
               } catch (e: Exception) {
                  context.logger.severe(
                     "error in converting message to ${targetType.fullyQualifiedName}, excluding from to be inserted items",
                     e
                  )
                  null
               }
            }
            val insertStatements = InsertStatementGenerator(schema).generateInserts(
               typedInstances,
               context.sqlDsl(),
               useUpsertSemantics = true
            )
            logger.info { "Executing INSERT batch with size: ${insertStatements.size}" }
            try {
               val insertedCount = context.sqlDsl().batch(insertStatements).execute()
               context.logger.info("inserted $insertedCount ${targetType.fullyQualifiedName} into DB")
            } catch (e: Exception) {
               context.logger.severe(
                  "Failed to insert ${insertStatements.size} ${targetType.fullyQualifiedName} into DB",
                  e
               )

            }
         }
         .build()
   }


}

@SpringAware
class JdbcSinkContext(
   val logger: ILogger,
   val outputSpec: JdbcTransportOutputSpec
) {

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

