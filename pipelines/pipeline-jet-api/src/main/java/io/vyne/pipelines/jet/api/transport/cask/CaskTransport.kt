package io.vyne.pipelines.jet.api.transport.cask

import io.vyne.VersionedTypeReference
import io.vyne.pipelines.jet.api.documentation.Maturity
import io.vyne.pipelines.jet.api.documentation.PipelineDocs
import io.vyne.pipelines.jet.api.documentation.PipelineDocumentationSample
import io.vyne.pipelines.jet.api.documentation.PipelineParam
import io.vyne.pipelines.jet.api.transport.PipelineDirection
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpec
import io.vyne.pipelines.jet.api.transport.PipelineTransportSpecId
import io.vyne.pipelines.jet.api.transport.PipelineTransportType
import io.vyne.schemas.fqn

object CaskTransport {
   const val TYPE: PipelineTransportType = "cask"
   val OUTPUT = CaskTransportOutputSpec.specId
}

@PipelineDocs(
   name = "Cask",
   docs = """An output that writes directly to a Vyne Cask.

Casks provide a way to store content in a database, and expose an auto-generated RESTful service over the top, with
all the correct Vyne operation schemas generated.

You may wish to consider using a Jdbc database output instead.
   """,
   maturity = Maturity.BETA,
   sample = CaskTransportOutputSpec.Sample::class
)
data class CaskTransportOutputSpec(
   @PipelineParam("The type that defines the cask containing this data")
   val targetType: VersionedTypeReference
) : PipelineTransportSpec {

   override val description: String = "Cask for $targetType"

   object Sample : PipelineDocumentationSample<CaskTransportOutputSpec> {
      override val sample = CaskTransportOutputSpec(
         targetType = VersionedTypeReference("com.demo.Customer".fqn())
      )

   }

   companion object {
      val specId =
         PipelineTransportSpecId(CaskTransport.TYPE, PipelineDirection.OUTPUT, CaskTransportOutputSpec::class.java)
   }

   override val direction: PipelineDirection
      get() = PipelineDirection.OUTPUT
   override val type: PipelineTransportType
      get() = CaskTransport.TYPE
}
