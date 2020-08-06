package io.vyne.pipelines

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus
import io.vyne.schemas.Type
import io.vyne.utils.log
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.io.InputStream
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
)

/**
 * Defines the parameters of a transport,
 * not the actual transport itself
 */
interface PipelineTransportSpec {
   val type: PipelineTransportType
   val direction: PipelineDirection
   val targetType: VersionedTypeReference
   val props: Map<String, Any>?
}

data class GenericPipelineTransportSpec(override val type: PipelineTransportType, override val direction: PipelineDirection, override val targetType: VersionedTypeReference, override val props: Map<String, String>?) : PipelineTransportSpec

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


interface PipelineTransort {

   /**
    * Pipeline health monitor
    */
   val healthMonitor: PipelineTransportHealthMonitor
      get() = AlwaysUpPipelineTransportMonitor()
}

/**
 * Maker interface for the actual IO pipe where we'll connect
 * eg., kafka / files / etc
 */
interface PipelineInputTransport : PipelineTransort {

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

sealed class PipelineMessage(val content: MessageContentProvider, val pipeline: Pipeline, val inputType: Type, val outputType: Type)
class TransformablePipelineMessage(content: MessageContentProvider, pipeline: Pipeline, inputType: Type, outputType: Type, val instance: TypedInstance, var transformedInstance: TypedInstance? = null) : PipelineMessage(content, pipeline, inputType, outputType)
class RawPipelineMessage(content: MessageContentProvider, pipeline: Pipeline, inputType: Type, outputType: Type) : PipelineMessage(content, pipeline, inputType, outputType)

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
   val contentProvider: MessageContentProvider
) {
   val id = messageTimestamp.toEpochMilli()
}


interface PipelineOutputTransport : PipelineTransort {

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
