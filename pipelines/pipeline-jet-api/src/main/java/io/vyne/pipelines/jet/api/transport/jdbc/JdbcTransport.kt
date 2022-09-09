package io.vyne.pipelines.jet.api.transport.jdbc

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import org.intellij.lang.annotations.Language

object JdbcTransport {
   const val TYPE: PipelineTransportType = "jdbc"
   val INPUT = JdbcTransportInputSpec.specId
   val OUTPUT = JdbcTransportOutputSpec.specId
}

/**
 * This isn't documented, as I suspect it's junk.
 * No tests, variables are misnamed (topic) and unclear how this would work, since it doesn't operate on a schedule.
 */
open class JdbcTransportInputSpec(
   val topic: String,
   val targetTypeName: String,
) : PipelineTransportSpec {

   companion object {
      val specId =
         PipelineTransportSpecId(JdbcTransport.TYPE, PipelineDirection.INPUT, JdbcTransportInputSpec::class.java)
   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(targetTypeName)
      }

   override val requiredSchemaTypes: List<String>
      get() = listOf(targetTypeName)

   override val description: String = "JDBC"
   override val direction: PipelineDirection
      get() = PipelineDirection.INPUT
   override val type: PipelineTransportType
      get() = JdbcTransport.TYPE
}


@PipelineDocs(
   name = "Database Output",
   docs = JdbcTransportOutputSpec.docs,
   maturity = Maturity.BETA,
   sample = JdbcTransportOutputSpec.Sample::class
)
data class JdbcTransportOutputSpec(
   @PipelineParam("The name of a connection, configured in Vyne's connection manager")
   val connection: String,
   @PipelineParam("The fully qualified name of the type which content being pushed to the database should be read as")
   val targetTypeName: String
) : WindowingPipelineTransportSpec {
   constructor(
      connection: String,
      targetType: VersionedTypeReference
   ) : this(connection, targetType.toString())

   object Sample : PipelineDocumentationSample<JdbcTransportOutputSpec> {
      override val sample = JdbcTransportOutputSpec(
         connection = "my-connection",
         targetTypeName = "com.demo.Customer"
      )

   }

   companion object {
      const val windowDurationMs: Long = 500
      val specId =
         PipelineTransportSpecId(JdbcTransport.TYPE, PipelineDirection.OUTPUT, JdbcTransportOutputSpec::class.java)

      @Language("Markdown")
      const val docs = """A pipeline output that writes to a database.

The pipeline uses a connection that has been defined using Vyne's connection manager.
Most database types are supported, providing they expose a JDBC driver.

#### Table definition
A table is created, if it doesn't already exist, using the config defined on the target type.

If the table contains a `io.vyne.jdbc.Table` annotation, then this is used
to define the table name.  Otherwise, a table name is derived from the
name of the type.

Similarly, columns are created for all attributes annotated with a `io.vyne.jdbc.Column` annotation.

The table creation occurs when the pipeline is first initiated, and run once.
Table creation occurs using a `CREATE IF NOT EXISTS` statement, so if the type
has been changed since the table was first created, changes will not be propagated
to the database.

#### Batching inserts
In order to reduce load on the database, inserts are batched in windows of ${windowDurationMs}ms.

      """


   }

   val targetType: VersionedTypeReference
      get() {
         return VersionedTypeReference.parse(this.targetTypeName)
      }

   override val requiredSchemaTypes: List<String>
      get() = listOf(targetTypeName)

   override val description: String = "Jdbc connection $connection"

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = JdbcTransport.TYPE
   override val windowDurationMs: Long = JdbcTransportOutputSpec.windowDurationMs
}
