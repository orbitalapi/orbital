package io.vyne.pipelines.jet.sink.redshift

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.pipelines.jet.api.transport.ConsoleLogger
import io.vyne.pipelines.jet.api.transport.MessageContentProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.redshift.RedshiftTransportOutputSpec
import io.vyne.pipelines.jet.pipelines.InstanceAttributeSet
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator
import io.vyne.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.spring.VyneProvider
import mu.KotlinLogging
import java.sql.DriverManager
import java.util.*
import javax.annotation.Resource


object RedshiftSink // for Logging
class RedshiftSinkBuilder() :
   SingleMessagePipelineSinkBuilder<RedshiftTransportOutputSpec> {

   val postgresDdlGenerator = PostgresDdlGenerator()
   lateinit var schema: Schema

   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineSpec: PipelineSpec<*, *>): Boolean =
      pipelineSpec.output is RedshiftTransportOutputSpec

   override fun getRequiredType(
      pipelineSpec: PipelineSpec<*, RedshiftTransportOutputSpec>,
      schema: Schema
   ): QualifiedName {
      this.schema = schema

      return pipelineSpec.output.targetType.typeName
   }

   override fun build(pipelineSpec: PipelineSpec<*, RedshiftTransportOutputSpec>): Sink<MessageContentProvider> {

      return SinkBuilder
         .sinkBuilder("redshift-sink") { context ->
            RedshiftSinkContext(
               context.logger(),
               pipelineSpec
            )
         }
         .receiveFn { context: RedshiftSinkContext, message: MessageContentProvider ->

            val postgresDdlGenerator = PostgresDdlGenerator()
            val vyne = context.vyneProvider.createVyne()
            val schema = vyne.schema
            val input = TypedInstance.from(schema.versionedType(pipelineSpec.output.targetType.typeName).type, message.asString(ConsoleLogger), schema)

            val instanceAttributeSet = InstanceAttributeSet(
               schema.versionedType(pipelineSpec.output.targetType.typeName),
               input as Map<String, TypedInstance>,
               UUID.randomUUID().toString()
            )

            val upsetMetaData = postgresDdlGenerator.generateUpsertDml(
               versionedType = schema.versionedType(pipelineSpec.output.targetType.typeName),
               instance = instanceAttributeSet,
               fetchOldValues = false
            )

            //Create target table if necessary // START REFACTOR
            val targetTable = postgresDdlGenerator.generateDdlRedshift(schema.versionedType(pipelineSpec.output.targetType.typeName), schema)
            val connectionConfiguration = context.jdbcConnectionRegistry.getConnection(context.outputSpec.connection)

            val urlCredentials = connectionConfiguration.buildUrlAndCredentials()
            val url = connectionConfiguration.buildUrlAndCredentials().url

            val connection = DriverManager.getConnection(url, urlCredentials.username, urlCredentials.password)
            val statement = connection.createStatement()

            statement.execute(targetTable.ddlStatement)
            //Create target table if necessary // END REFACTOR


            val ret = connection.createStatement().executeUpdate(upsetMetaData.upsertSqlStatement)

         }
         .build()
   }

}

@SpringAware
class RedshiftSinkContext(
   val logger: ILogger,
   val pipelineSpec: PipelineSpec<*, RedshiftTransportOutputSpec>
) {
   val outputSpec: RedshiftTransportOutputSpec = pipelineSpec.output

   @Resource
   lateinit var vyneProvider: VyneProvider

   @Resource
   lateinit var jdbcConnectionRegistry: InMemoryJdbcConnectionRegistry
}

