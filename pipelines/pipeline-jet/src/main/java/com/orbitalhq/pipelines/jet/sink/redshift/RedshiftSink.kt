package com.orbitalhq.pipelines.jet.sink.redshift

import com.hazelcast.jet.pipeline.Sink
import com.hazelcast.jet.pipeline.SinkBuilder
import com.hazelcast.logging.ILogger
import com.hazelcast.spring.context.SpringAware
import com.orbitalhq.VyneClientWithSchema
import com.orbitalhq.connectors.jdbc.buildUrlAndCredentials
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.pipelines.jet.api.transport.MessageContentProvider
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.redshift.RedshiftTransportOutputSpec
import com.orbitalhq.pipelines.jet.pipelines.InstanceAttributeSet
import com.orbitalhq.pipelines.jet.pipelines.PostgresDdlGenerator
import com.orbitalhq.pipelines.jet.sink.SingleMessagePipelineSinkBuilder
import com.orbitalhq.schemas.QualifiedName
import com.orbitalhq.schemas.Schema
import jakarta.annotation.Resource
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.sql.DriverManager
import java.util.*

@Component
class RedshiftSinkBuilder :
   SingleMessagePipelineSinkBuilder<RedshiftTransportOutputSpec> {

   lateinit var schema: Schema

   companion object {
      val logger = KotlinLogging.logger { }
   }

   override fun canSupport(pipelineTransportSpec: PipelineTransportSpec): Boolean =
      pipelineTransportSpec is RedshiftTransportOutputSpec

   override fun getRequiredType(
      pipelineTransportSpec: RedshiftTransportOutputSpec,
      schema: Schema
   ): QualifiedName {
      this.schema = schema

      return pipelineTransportSpec.targetType.typeName
   }

   override fun build(
      pipelineId: String,
      pipelineName: String,
      pipelineTransportSpec: RedshiftTransportOutputSpec
   ): Sink<MessageContentProvider> {

      return SinkBuilder
         .sinkBuilder("redshift-sink") { context ->
            RedshiftSinkContext(
               context.logger(),
               pipelineTransportSpec
            )
         }
         .receiveFn { context: RedshiftSinkContext, message: MessageContentProvider ->

            val postgresDdlGenerator = PostgresDdlGenerator()
            val schema = context.vyneClient.schema
            val input = TypedInstance.from(
               schema.versionedType(pipelineTransportSpec.targetType.typeName).type,
               message.asString(),
               schema
            )

            val instanceAttributeSet = InstanceAttributeSet(
               schema.versionedType(pipelineTransportSpec.targetType.typeName),
               input as Map<String, TypedInstance>,
               UUID.randomUUID().toString()
            )

            val upsertMetadata = postgresDdlGenerator.generateUpsertDml(
               versionedType = schema.versionedType(pipelineTransportSpec.targetType.typeName),
               instance = instanceAttributeSet,
               fetchOldValues = false
            )

            //Create target table if necessary // START REFACTOR
            val targetTable = postgresDdlGenerator.generateDdlRedshift(
               schema.versionedType(pipelineTransportSpec.targetType.typeName),
               schema
            )
            val connectionConfiguration = context.jdbcConnectionRegistry.getConnection(context.outputSpec.connection)

            val urlCredentials = connectionConfiguration.buildUrlAndCredentials()
            val url = urlCredentials.url

            val connection = DriverManager.getConnection(url, urlCredentials.username, urlCredentials.password)
            val statement = connection.createStatement()

            statement.execute(targetTable.ddlStatement)
            connection.createStatement().executeUpdate(upsertMetadata.upsertSqlStatement)
         }
         .build()
   }

}

@SpringAware
class RedshiftSinkContext(
   val logger: ILogger,
   val outputSpec: RedshiftTransportOutputSpec
) {
   @Resource
   lateinit var vyneClient: VyneClientWithSchema

   @Resource
   lateinit var jdbcConnectionRegistry: InMemoryJdbcConnectionRegistry
}

