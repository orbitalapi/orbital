package com.orbitalhq.pipelines.jet.api.transport.query

import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocumentationSample
import com.orbitalhq.pipelines.jet.api.documentation.PipelineParam
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType

object StreamingQueryTransport {
   const val TYPE: PipelineTransportType = "streamingQuery"
   val INPUT = StreamingQueryInputSpec.specId
}

class StreamingQueryInputSpec(
   @PipelineParam("The query to be executed. See the sample for an example. ")
   override val query: String,
) : PipelineTransportSpec, TaxiQlQueryPipelineTransportSpec {
   object Sample : PipelineDocumentationSample<StreamingQueryInputSpec> {
      override val sample = StreamingQueryInputSpec(
         query = "stream { Tweets( Username == 'Jim' ) }",
      )
   }

   companion object {
      val specId = PipelineTransportSpecId(
         StreamingQueryTransport.TYPE,
         PipelineDirection.INPUT,
         StreamingQueryInputSpec::class.java
      )
   }
   override val type: PipelineTransportType = StreamingQueryTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT

   override val description: String =
      "Execute the streaming query ${query.replace("\n", " ")}"
}
