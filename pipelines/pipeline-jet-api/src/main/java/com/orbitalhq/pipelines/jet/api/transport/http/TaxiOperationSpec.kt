package com.orbitalhq.pipelines.jet.api.transport.http

import com.orbitalhq.pipelines.jet.api.documentation.Maturity
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocs
import com.orbitalhq.pipelines.jet.api.documentation.PipelineDocumentationSample
import com.orbitalhq.pipelines.jet.api.documentation.PipelineParam
import com.orbitalhq.pipelines.jet.api.transport.CronExpression
import com.orbitalhq.pipelines.jet.api.transport.ParameterMap
import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn

/**
 * Transport that invokes an operation, as declared in a taxi schema
 * All details about the operation are read from the schema
 */
object TaxiOperationTransport {
   const val TYPE: PipelineTransportType = "taxiOperation"
   val INPUT = PollingTaxiOperationInputSpec.specId
   val OUTPUT = TaxiOperationOutputSpec.specId
}

object CronExpressions {
   const val EVERY_SECOND = "* * * * * *"
   const val EVERY_TEN_SECONDS = "*/10 * * * * *"
}

@PipelineDocs(
   name = "Polling operation input",
   docs = """
Invokes an operation (as defined or published to Vyne), on a periodic basis.

Accepts inputs defined in the configuration, which will be passed to the service on invocation.
The result of this operation is published downstream on the pipeline to be transformed to
another type, and published to an output.
   """,
   maturity = Maturity.BETA,
   sample = PollingTaxiOperationInputSpec.Sample::class
)
data class PollingTaxiOperationInputSpec(
   @PipelineParam("The name of the operation, as defined in the schema.  Should be in the format of a fully qualified operation name.  See the sample for an example")
   val operationName: String,
   @PipelineParam("A [Spring-flavored cron expression](https://www.baeldung.com/cron-expressions#cron-expression), defining the frequency this operation should be invoked.")
   val pollSchedule: CronExpression,
   @PipelineParam("An optional map of parameters to pass to the operation")
   val parameterMap: ParameterMap = emptyMap()
) : PipelineTransportSpec {
   object Sample : PipelineDocumentationSample<PollingTaxiOperationInputSpec> {
      override val sample = PollingTaxiOperationInputSpec(
         operationName = OperationNames.name("com.demo.customers.CustomerService", "listCustomers"),
         pollSchedule = CronExpressions.EVERY_SECOND,
         parameterMap = mapOf(
            "customerStatus" to "ACTIVE"
         )
      )
   }

   companion object {
      val specId = PipelineTransportSpecId(
         TaxiOperationTransport.TYPE,
         PipelineDirection.INPUT,
         PollingTaxiOperationInputSpec::class.java
      )
   }

   override val type: PipelineTransportType = TaxiOperationTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.INPUT
   override val description: String =
      "Fetch data from operation ${OperationNames.displayNameFromOperationName(operationName.fqn())}"

}

@PipelineDocs(
   name = "Operation output",
   docs = """
Invokes an operation (as defined or published to Vyne), using the data
provided upstream in the pipeline.

If the provided data does not satisfy the contract of the operation,
Vyne will use the provided input as the basis of a discovery search, to find
additional data.
   """,
   maturity = Maturity.BETA,
   sample = TaxiOperationOutputSpec.Sample::class
)
data class TaxiOperationOutputSpec(
   @PipelineParam("The name of the operation, as defined in the schema.  Should be in the format of a fully qualified operation name.  See the sample for an example")
   val operationName: String
) : PipelineTransportSpec {

   object Sample : PipelineDocumentationSample<TaxiOperationOutputSpec> {
      override val sample = TaxiOperationOutputSpec(
         operationName = OperationNames.name("com.demo.customers.CustomerService", "DisableCustomerAccounts")
      )
   }

   companion object {
      val specId = PipelineTransportSpecId(
         TaxiOperationTransport.TYPE,
         PipelineDirection.OUTPUT,
         TaxiOperationOutputSpec::class.java
      )
   }

   override val type: PipelineTransportType = TaxiOperationTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val description: String =
      "Send data to operation ${OperationNames.displayNameFromOperationName(operationName.fqn())}"

}
