package io.vyne.pipelines.runner.transport.http

import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.ParameterMap
import io.vyne.pipelines.runner.transport.PipelineTransportSpecId
import io.vyne.schemas.OperationNames
import io.vyne.schemas.fqn

/**
 * Transport that invokes an operation, as declared in a taxi schema
 * All details about the operation are read from the schema
 */
object TaxiOperationTransport {
   const val TYPE: PipelineTransportType = "taxiOperation"
   val INPUT = PollingTaxiOperationInputSpec.specId
   val OUTPUT = TaxiOperationOutputSpec.specId
}

typealias CronExpression = String

object CronExpressions {
   const val EVERY_SECOND =  "* * * * * *"
   const val EVERY_TEN_SECONDS =  "*/10 * * * * *"
}

data class PollingTaxiOperationInputSpec(
   val operationName: String,
   val pollSchedule: CronExpression,
   val parameterMap: ParameterMap = emptyMap()
) : PipelineTransportSpec {
   companion object {
      val specId = PipelineTransportSpecId(
         TaxiOperationTransport.TYPE,
         PipelineDirection.INPUT,
         PollingTaxiOperationInputSpec::class.java
      )
   }

   override val type: PipelineTransportType = TaxiOperationTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Fetch data from operation ${OperationNames.displayNameFromOperationName(operationName.fqn())}"

}

data class TaxiOperationOutputSpec(
   val operationName: String
) : PipelineTransportSpec {

   companion object {
      val specId = PipelineTransportSpecId(
         TaxiOperationTransport.TYPE,
         PipelineDirection.OUTPUT,
         TaxiOperationOutputSpec::class.java
      )
   }

   override val type: PipelineTransportType = TaxiOperationTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val props: Map<String, Any> = emptyMap()
   override val description: String = "Send data to operation ${OperationNames.displayNameFromOperationName(operationName.fqn())}"

}
