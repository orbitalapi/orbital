package io.vyne.pipelines.jet.api.transport

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.aws.s3.AwsS3TransportOutputSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.cask.CaskTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.http.HttpListenerTransportSpec
import io.vyne.pipelines.jet.api.transport.http.PollingTaxiOperationInputSpec
import io.vyne.pipelines.jet.api.transport.http.TaxiOperationOutputSpec
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import io.vyne.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec
import io.vyne.pipelines.jet.api.transport.query.PollingQueryInputSpec
import io.vyne.utils.orElse

val availableSpecs = listOf(
   KafkaTransportInputSpec.specId,
   CaskTransportOutputSpec.specId,
   KafkaTransportOutputSpec.specId,
   HttpListenerTransportSpec.specId,
   TaxiOperationOutputSpec.specId,
   PollingTaxiOperationInputSpec.specId,
   AwsSqsS3TransportInputSpec.specId,
   AwsS3TransportInputSpec.specId,
   AwsS3TransportOutputSpec.specId,
   JdbcTransportOutputSpec.specId,
   PollingQueryInputSpec.specId
)

class PipelineJacksonModule(
   ids: List<PipelineTransportSpecId> = availableSpecs
) : SimpleModule() {

   init {
      addDeserializer(PipelineTransportSpec::class.java, PipelineTransportSpecDeserializer(ids))
   }

   companion object {
      val pipelineTransportSpecs = availableSpecs
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

class PipelineListTransportSpecDeserializer(val ids: List<PipelineTransportSpecId> = PipelineJacksonModule.pipelineTransportSpecs) :
   JsonDeserializer<List<PipelineTransportSpec>>() {
   private val innerJackson = jacksonObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

   override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<PipelineTransportSpec> {
      val list = p.readValueAs(List::class.java) as List<Map<String, Any>>
      return list.map {
         val type = it["type"] as? String ?: error("Property 'type' was expected")
         val direction = it["direction"] as? String ?: error("Property 'direction' was expected")
         val pipelineDirection = PipelineDirection.valueOf(direction)

         val clazz = ids
            .filter { it.type == type }.firstOrNull { it.direction == pipelineDirection }
            ?.clazz
            .orElse(GenericPipelineTransportSpec::class.java)

         innerJackson.convertValue(it, clazz)
      }
   }
}

data class PipelineTransportSpecId(
   val type: PipelineTransportType,
   val direction: PipelineDirection,
   val clazz: Class<out PipelineTransportSpec>
)
