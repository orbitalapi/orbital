package io.vyne.pipelines.runner.transport

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.pipelines.PipelineDirection
import io.vyne.pipelines.PipelineTransportSpec
import io.vyne.pipelines.PipelineTransportType
import io.vyne.pipelines.runner.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.runner.transport.kafka.KafkaTransportOutputSpec

class PipelineJacksonModule : SimpleModule() {

   init {
      addDeserializer(PipelineTransportSpec::class.java, PipelineTransportSpecDeserializer())
   }
}

class PipelineTransportSpecDeserializer(private val ids: List<PipelineTransportSpecId> = listOf(
   KafkaTransportInputSpec.specId,
   CaskTransportOutputSpec.specId,
   KafkaTransportOutputSpec.specId
)) : JsonDeserializer<PipelineTransportSpec>() {
   private val innerJackson = jacksonObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PipelineTransportSpec {
      val map = p.readValueAs(Map::class.java) as Map<String, Any>
      val type = map["type"] as? String ?: error("Property 'type' was expected")
      val direction = map["direction"] as? String ?: error("Property 'direction' was expected")
      val pipelineDirection = PipelineDirection.valueOf(direction)
      val specId = ids.firstOrNull { it.type == type && it.direction == pipelineDirection }
         ?: error("No spec type matches type $type and direction $direction")

      val result = innerJackson.convertValue(map, specId.clazz)
      return result
   }

}

data class PipelineTransportSpecId(val type: PipelineTransportType, val direction: PipelineDirection, val clazz: Class<out PipelineTransportSpec>)
