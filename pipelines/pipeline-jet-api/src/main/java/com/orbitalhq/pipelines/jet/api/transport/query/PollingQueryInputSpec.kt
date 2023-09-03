package com.orbitalhq.pipelines.jet.api.transport.query

import com.orbitalhq.pipelines.jet.api.documentation.Maturity
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocs
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocumentationSample
import com.orbitalhq.pipelines.jet.api.documentation.PipelineParam
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.api.transport.ScheduledPipelineTransportSpec

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
   const val EVERY_MINUTE = "0 * * * * *"
   const val EVERY_HOUR = "* 0 * * * *"
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
   @PipelineParam("A [Spring-flavored cron expression](https://www.baeldung.com/cron-expressions#cron-expression), defining the frequency this query should be invoked.")
   override val pollSchedule: CronExpression,
   @PipelineParam("When set to true, specifically controls the next execution time when the last execution finishes.")
   override val preventConcurrentExecution: Boolean = false
) : ScheduledPipelineTransportSpec {
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