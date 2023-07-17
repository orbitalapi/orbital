package io.vyne.pipelines.jet.api.transport.gcp.pubsub

import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.*
import io.vyne.pipelines.jet.api.transport.http.CronExpressions

object GcpStoragePubSubTransport {
   const val TYPE: PipelineTransportType = "gcpPubSubStorage"
   val INPUT = GcpStoragePubSubTransportInputSpec.specId
}

@PipelineDocs(
   name = "GCP Storage vis PubSub",
   docs = """A source that consumes a stream of GCP PubSub events via a preconfigured subscription""",
   sample = GcpStoragePubSubTransportInputSpec.Sample::class,
   maturity = Maturity.EXPERIMENTAL
)
data class GcpStoragePubSubTransportInputSpec(
   @PipelineParam("The name of the connection, as registered in Orbital's connection manager")
   val connection: String,
   @PipelineParam("The name of the type that content from the Google Storage bucket should be consumed as")
   val targetTypeName: String,
   @PipelineParam("The name of the PubSub subscription")
   val subscriptionName: String,
   @PipelineParam("The Id of the GCP project")
   val projectId: String,
//   @PipelineParam("A cron expression that defines how frequently to check for new messages. Defaults to every second.")
//   override val pollSchedule: CronExpression = CronExpressions.EVERY_SECOND,
//   @PipelineParam("When set to true, specifically controls the next execution time when the last execution finishes.")
//   override val preventConcurrentExecution: Boolean = false
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(
            GcpStoragePubSubTransport.TYPE,
            PipelineDirection.INPUT,
            GcpStoragePubSubTransportInputSpec::class.java
         )
   }


   object Sample : PipelineDocumentationSample<PipelineTransportSpec> {
      override val sample: PipelineTransportSpec = GcpStoragePubSubTransportInputSpec(
         "my-gcp-connection",
         "com.demo.customers.Customer",
         "customer-events",
         "* * * * * *"
      )

   }

   override val type: PipelineTransportType = GcpStoragePubSubTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String = "Gcp Storage via PubSub"
}
