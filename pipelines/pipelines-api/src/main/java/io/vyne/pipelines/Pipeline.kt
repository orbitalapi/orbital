package io.vyne.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus
import io.vyne.schemas.Type
import io.vyne.utils.log
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.io.OutputStream
import java.time.Instant
import kotlin.math.absoluteValue

const val PIPELINE_METADATA_KEY = "pipeline"

data class Pipeline(
   val name: String,
   val input: PipelineChannel,
   val output: PipelineChannel
) {
   val id = "$name@${hashCode().absoluteValue}"
}

data class PipelineChannel(
   val type: VersionedTypeReference,
   val transport: PipelineTransportSpec
) {
   val description:String = transport.description
}

/**
 * Defines the parameters of a transport,
 * not the actual transport itself
 */
interface PipelineTransportSpec {
   val type: PipelineTransportType
   val direction: PipelineDirection
   val targetType: VersionedTypeReference
   val props: Map<String, Any>?

   /**
    * A human, log-friendly description of this spec
    */
   val description: String
}

data class GenericPipelineTransportSpec(override val type: PipelineTransportType, override val direction: PipelineDirection, override val targetType: VersionedTypeReference, override val props: Map<String, String>?) : PipelineTransportSpec {
   override val description: String = "Pipeline $direction $type"
}

enum class PipelineDirection(val label: String) {
   INPUT("in"),
   OUTPUT("out");

   companion object {
      fun from(label: String): PipelineDirection {
         return when (label) {
            INPUT.label -> INPUT
            OUTPUT.label -> OUTPUT
            else -> error("Unknown label: $label")
         }
      }
   }

}
typealias PipelineTransportType = String


interface PipelineTransport {

   /**
    * Pipeline health monitor
    */
   val healthMonitor: PipelineTransportHealthMonitor
      get() = AlwaysUpPipelineTransportMonitor()

   /**
    * A human, log-friendly description of this pipeline.
    * Generally, defer to the PipelineTransportSpec.description
    */
   val description: String
}

/**
 * Maker interface for the actual IO pipe where we'll connect
 * eg., kafka / files / etc
 */
interface PipelineInputTransport : PipelineTransport {

   /**
    * Input feed of messages
    */
   val feed: Flux<PipelineInputMessage>

   /**
    * Pause the input events ingestion
    */
   fun pause() {}

   /**
    * Resume the input events ingestion
    */
   fun resume() {}
}

sealed class PipelineMessage {
   abstract val content: MessageContentProvider
   abstract val pipeline: Pipeline
   abstract val inputType: Type
   abstract val outputType: Type
   /**
    * Allows a message to override the target destination.
    * If not provided, then the destination defined in the pipeline will be used
    */
   abstract val overrideOutput:PipelineOutputTransport?
}

data class TransformablePipelineMessage(
   override val content: MessageContentProvider,
   override val pipeline: Pipeline,
   override val inputType: Type,
   override val outputType: Type,
   val instance: TypedInstance,
   val transformedInstance: TypedInstance? = null,
   /**
    * Allows a message to override the target destination.
    * If not provided, then the destination defined in the pipeline will be used
    */
   override val overrideOutput:PipelineOutputTransport? = null
) : PipelineMessage()

data class RawPipelineMessage(
   override val content: MessageContentProvider,
   override val pipeline: Pipeline,
   override val inputType: Type,
   override val outputType: Type,
   /**
    * Allows a message to override the target destination.
    * If not provided, then the destination defined in the pipeline will be used
    */
   override val overrideOutput:PipelineOutputTransport? = null
) : PipelineMessage()

interface MessageContentProvider {
   fun asString(logger: PipelineLogger): String
   fun writeToStream(logger: PipelineLogger, outputStream: OutputStream)
}


data class JacksonContentProvider(private val objectMapper: ObjectMapper, private val content: Any) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return objectMapper.writeValueAsString(content)
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      objectMapper.writeValue(outputStream, content)
   }
}

data class StringContentProvider(val content: String) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return content
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      outputStream.write(content.toByteArray())
   }
}

data class PipelineInputMessage(
   // Publishers should try to use the time that the
   // message was produced, not the time the consumer
   // has received it
   val messageTimestamp: Instant,
   val metadata: Map<String, Any> = emptyMap(),
   val contentProvider: MessageContentProvider,
   /**
    * Allows a message to override the target destination.
    * If not provided, then the destination defined in the pipeline will be used
    */
   val overrideOutput:PipelineOutputTransport? = null

) {
   val id = messageTimestamp.toEpochMilli()
}


interface PipelineOutputTransport : PipelineTransport {

   val type: VersionedTypeReference
   fun write(message: MessageContentProvider, logger: PipelineLogger)

}

class AlwaysUpPipelineTransportMonitor : PipelineTransportHealthMonitor


interface PipelineTransportHealthMonitor {

   /**
    * Pipeline Transport Status feed
    */
   val healthEvents
      get() = Flux.just(PipelineTransportStatus.UP)

   /**
    * Report a new status changes.
    */
   fun reportStatus(status: PipelineTransportStatus) {}

   /**
    * Transports' status
    */
   enum class PipelineTransportStatus {

      // Transport is initialising for the first time
      INIT,

      // Transport is up and ready to receive events
      UP,

      // Transport is down (for any reason) but can be recovered
      DOWN,

      // Transport is down and can't be recovered
      TERMINATED
   }
}

/**
 * Default PipelineTransportHealthMonitor implementation, using an EmitterProcessor
 */
open class EmitterPipelineTransportHealthMonitor : PipelineTransportHealthMonitor {

   private val processor: EmitterProcessor<PipelineTransportStatus> = EmitterProcessor.create()
   private val sink = processor.sink()

   override val healthEvents = processor
   override fun reportStatus(status: PipelineTransportStatus) {
      sink.next(status)
   }

}


interface PipelineLogger {
   fun debug(message: () -> String)
   fun info(message: () -> String)
   fun warn(message: () -> String)
   fun error(message: () -> String)
   fun error(exception: Throwable, message: () -> String)
}

class ConsoleLogger : PipelineLogger {
   override fun debug(message: () -> String) {
      log().debug(message())
   }

   override fun info(message: () -> String) {
      log().info(message())
   }

   override fun warn(message: () -> String) {
      log().warn(message())
   }

   override fun error(message: () -> String) {
      log().error(message())
   }

   override fun error(exception: Throwable, message: () -> String) {
      log().error(message(), exception)
   }

}
