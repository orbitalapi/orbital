package io.vyne.pipelines.jet.api.transport.query

import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType

/**
 * Transport that invokes a TaxiQL query on Vyne
 */
object QueryTransport {
   const val TYPE: PipelineTransportType = "query"
   val INPUT = PollingQueryInputSpec.specId
}

typealias CronExpression = String

object CronExpressions {
   const val EVERY_SECOND = "* * * * * *"
}

@PipelineDocs(
   name = "Polling query input",
   docs = """
Invokes a TaxiQL query on Vyne, on a periodic basis.

The result of this query is published downstream on the pipeline to be transformed to
another type, and published to an output.
   """,
   maturity = Maturity.BETA,
   sample = PollingQueryInputSpec.Sample::class
)
data class PollingQueryInputSpec(
   @PipelineParam("The query to be executed. See the sample for an example. ")
   val query: String,
   @PipelineParam("A [cron expression](https://www.baeldung.com/cron-expressions#cron-expression), defining the frequency this query should be invoked.")
   val pollSchedule: CronExpression,
) : PipelineTransportSpec {
   object Sample : PipelineDocumentationSample<PollingQueryInputSpec> {
      override val sample = PollingQueryInputSpec(
         query = "find { Person( FirstName == 'Jim' ) }",
         pollSchedule = CronExpressions.EVERY_SECOND
      )
   }

   companion object {
      val specId = PipelineTransportSpecId(
         QueryTransport.TYPE,
         PipelineDirection.INPUT,
         PollingQueryInputSpec::class.java
      )
   }

   override val type: PipelineTransportType = QueryTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String =
      "Execute the query ${query.replace("\n", " ")}"

}
