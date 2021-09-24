package io.vyne.pipelines.jet.api.transport

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.pipelines.jet.api.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.http.HttpListenerTransportSpec
import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.utils.orElse

class PipelineJacksonModule(
   ids: List<PipelineTransportSpecId> = listOf(
      KafkaTransportInputSpec.specId,
      CaskTransportOutputSpec.specId,
      KafkaTransportOutputSpec.specId,
      HttpListenerTransportSpec.specId,
      TaxiOperationOutputSpec.specId,
      PollingTaxiOperationInputSpec.specId
   )
) : SimpleModule() {

   init {
      addDeserializer(PipelineTransportSpec::class.java, PipelineTransportSpecDeserializer(ids))
   }
   companion object {
      val pipelineTransportSpecs = listOf(
         KafkaTransportInputSpec.specId,
         CaskTransportOutputSpec.specId,
         KafkaTransportOutputSpec.specId,
         HttpListenerTransportSpec.specId,
         TaxiOperationOutputSpec.specId,
         PollingTaxiOperationInputSpec.specId
      )
   }
}

class PipelineTransportSpecDeserializer(val ids: List<PipelineTransportSpecId> = PipelineJacksonModule.pipelineTransportSpecs) :
   JsonDeserializer<PipelineTransportSpec>() {
   private val innerJackson = jacksonObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PipelineTransportSpec {
      val map = p.readValueAs(Map::class.java) as Map<String, Any>
      val type = map["type"] as? String ?: error("Property 'type' was expected")
      val direction = map["direction"] as? String ?: error("Property 'direction' was expected")
      val pipelineDirection = PipelineDirection.valueOf(direction)

      val clazz = ids
         .filter { it.type == type }.firstOrNull { it.direction == pipelineDirection }
         ?.clazz
         .orElse(GenericPipelineTransportSpec::class.java)

      return innerJackson.convertValue(map, clazz)
   }


}

data class PipelineTransportSpecId(
   val type: PipelineTransportType,
   val direction: PipelineDirection,
   val clazz: Class<out PipelineTransportSpec>
)
