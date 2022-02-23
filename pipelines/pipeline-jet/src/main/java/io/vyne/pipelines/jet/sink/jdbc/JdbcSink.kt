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
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.redshift.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.pipelines.jet.sink.WindowingPipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import org.jooq.DSLContext
import org.springframework.jdbc.core.JdbcTemplate
import javax.annotation.Resource


class JdbcSinkBuilder() :
   WindowingPipelineSinkBuilder<JdbcTransportOutputSpec> {
   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean = pipelineSpec.output is JdbcTransportOutputSpec

   override fun getRequiredType(
      pipelineSpec: PipelineSpec<*, JdbcTransportOutputSpec>,
      schema: Schema
   ): QualifiedName {
      return pipelineSpec.output.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<*, JdbcTransportOutputSpec>): Sink<WindowResult<List<MessageContentProvider>>> {
      return SinkBuilder
         .sinkBuilder("jdbc-sink") { context ->
            val sinkContext = context.managedContext().initialize(
               JdbcSinkContext(
                  context.logger(),
                  pipelineSpec
               )
            ) as JdbcSinkContext

            // Create the target table if it doesnt exist
            val schema = sinkContext.schema()
            val (tableName, ddlStatement) = TableGenerator(schema)
               .generate(schema.type(pipelineSpec.output.targetType), sinkContext.sqlDsl())
            context.logger()
               .info("Executing CREATE IF NOT EXISTS for table to store type ${pipelineSpec.output.targetTypeName} as table $tableName")

            context.logger().fine(ddlStatement.sql)
            ddlStatement.execute()
            val tableFoundAtDatabase = DatabaseMetadataService(sinkContext.jdbcTemplate())
               .listTables()
               .any { it.tableName.equals(tableName, ignoreCase = true) }
            if (tableFoundAtDatabase) {
               context.logger()
                  .info("Table $tableName created")
            } else {
               context.logger()
                  .severe("Failed to create database table $tableName.  No error was thrown, but the table was not found in the schema after the statement executed")
            }

            sinkContext
         }
         .receiveFn { context: JdbcSinkContext, message: WindowResult<List<MessageContentProvider>> ->
            val schema = context.schema()
            val targetType = schema.type(pipelineSpec.output.targetType)
            val typedInstances = message.result().map { messageContentProvider ->
               TypedInstance.from(targetType, messageContentProvider.asString(ConsoleLogger), schema)
            }
            InsertStatementGenerator(schema).generateInsert(typedInstances, context.sqlDsl())
               .execute()
         }
         .build()
   }

}

@SpringAware
class JdbcSinkContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<*, JdbcTransportOutputSpec>
) {
   val outputSpec: JdbcTransportOutputSpec = pipelineSpec.output

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

