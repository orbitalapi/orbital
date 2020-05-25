package io.vyne.pipelines

import io.vyne.VersionedTypeReference
import io.vyne.models.TypedInstance
import io.vyne.pipelines.PipelineTransportHealthMonitor.PipelineTransportStatus
import io.vyne.utils.log
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import java.time.Instant
import kotlin.math.absoluteValue

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
}

data class GenericPipelineTransportSpec(override val type: PipelineTransportType, override val direction: PipelineDirection, override val targetType: VersionedTypeReference) : PipelineTransportSpec

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

sealed class PipelineMessage(val content: String)
class TransformablePipelineMessage(content: String, val instance: TypedInstance, var transformedInstance: TypedInstance? = null)  : PipelineMessage(content)
class RawPipelineMessage( content: String): PipelineMessage(content)


data class PipelineInputMessage(
   // Publishers should try to use the time that the
   // message was produced, not the time the consumer
   // has received it
   val messageTimestamp: Instant,
   val metadata: Map<String, Any> = emptyMap(),
   val messageProvider: (logger: PipelineLogger) -> String
) {
   val id = messageTimestamp.toEpochMilli()
}


interface PipelineOutputTransport : PipelineTransort {

   val type: VersionedTypeReference
   fun write(message: String, logger: PipelineLogger)

}

class AlwaysUpPipelineTransportMonitor: PipelineTransportHealthMonitor


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
