package com.orbitalhq.pipelines.jet.api.transport.log

import com.orbitalhq.pipelines.jet.api.transport.PipelineDirection
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpec
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportSpecId
import com.orbitalhq.pipelines.jet.api.transport.PipelineTransportType
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.kafka.KafkaTransportOutputSpec

object LoggingTransport {
   const val TYPE: PipelineTransportType = "log"
   val INPUT = KafkaTransportInputSpec.specId
   val OUTPUT = KafkaTransportOutputSpec.specId
}

class LoggingOutputSpec(
   val logLevel: LogLevel,
   val logger: String,
   val captureForTest:Boolean = false
) : PipelineTransportSpec {
   companion object {
      val specId =
         PipelineTransportSpecId(LoggingTransport.TYPE, PipelineDirection.OUTPUT, LoggingOutputSpec::class.java)

      val captureForTest = LoggingOutputSpec(LogLevel.INFO, "Test", true)
   }

   override val type: PipelineTransportType = LoggingTransport.TYPE
   override val direction: PipelineDirection = PipelineDirection.OUTPUT
   override val description: String = "Logging output"
}

enum class LogLevel {
   TRACE,
   DEBUG,
   INFO,
   WARN,
   ERROR,
   OFF
}

