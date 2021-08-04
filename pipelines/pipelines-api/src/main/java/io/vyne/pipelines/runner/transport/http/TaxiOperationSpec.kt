package io.vyne.pipelines.runner.transport.http

import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId

/**
 * Transport that invokes an operation, as declared in a taxi schema
 * All details about the operation are read from the schema
 */
object TaxiOperationTransport {
   const val TYPE: PipelineTransportType = "taxiOperation"
   val OUTPUT = TaxiOperationSpec.specId
}


data class TaxiOperationSpec(
   val operationName: String
): PipelineTransportSpec {

   companion object {
      val specId = PipelineTransportSpecId(
         TaxiOperationTransport.TYPE,
         PipelineDirection.INPUT,
         TaxiOperationSpec::class.java
      )
   }

   override val type: PipelineTransportType = TaxiOperationTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Invoke operation $operationName"

}
