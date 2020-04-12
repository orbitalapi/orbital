package io.vyne.pipelines

import io.vyne.VersionedTypeReference
import reactor.core.publisher.Flux
import java.io.ByteArrayInputStream
import java.io.InputStream

data class Pipeline(
   val name: String,
   val input: PipelineChannel,
   val output: PipelineChannel
)

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
}

enum class PipelineDirection {
   INPUT,
   OUTPUT
}

typealias PipelineTransportType = String

/**
 * Maker interface for the actual IO pipe where we'll connect
 * eg., kafka / files / etc
 */
interface PipelineInputTransport {
   val feed: Flux<PipelineInputMessage>

   fun readAs(input:InputStream, type:VersionedTypeReference) // TODO :  Pausing here to extract types
}

data class PipelineInputMessage(
   val inputStream: InputStream,
   val metadata: Map<String,Any> = emptyMap()
)


interface PipelineOutputTransport {
   val type: VersionedTypeReference
   fun write(typedInstance: Any)
}
