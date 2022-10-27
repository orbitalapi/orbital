package io.vyne.pipelines.jet.api.transport.jdbc

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.pipelines.jet.api.transport.WindowingPipelineTransportSpec
import org.intellij.lang.annotations.Language

object JdbcTransport {
   const val TYPE: PipelineTransportType = "jdbc"
   val OUTPUT = JdbcTransportOutputSpec.specId
}

enum class WriteDisposition(val value: String) {
   APPEND("APPEND"),
   RECREATE("RECREATE")
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
   val targetTypeName: String,
   @PipelineParam("Whether to append new data into the existing table (APPEND), or to create a new table with a unique name and switch over the view to point to the newly created table (RECREATE).")
   val writeDisposition: WriteDisposition = WriteDisposition.APPEND,
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

#### Write disposition
Different pipelines have different needs in terms of what should be the result of subsequent runs. In some cases it is preferable to append the new data into the same table. For example when the new data is an increment to the existing dataset. In other cases it makes sense to replace the data with the new batch which is preferable when data always contains the full dataset. There are two supported write disposition modes to cater for different needs:
- `APPEND`: The data is appended to the existing table on each run of the pipeline. This is the default.
- `RECREATE`: The data is written to a new table, and the view is switched to point to the new table. It is the responsibility of the user to ensure that there aren't two concurrent runs as this introduces a race condition for the switch over and especially stale table deletion if enabled. The source needs to support this by providing a unique identifier for each run for it to work. In general, this only works for batch sources as the streaming sources don't ever complete. Currently only the AWS SQS S3 and polling query sources supports this.
The default write disposition is `APPEND`.
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
