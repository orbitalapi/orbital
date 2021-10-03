package io.vyne.pipelines.jet.api.transport

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.annotations.VisibleForTesting
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import java.io.OutputStream
import java.io.Serializable
import kotlin.math.absoluteValue

data class PipelineSpec<I : PipelineTransportSpec,O : PipelineTransportSpec>(
   val name: String,
   @JsonDeserialize(using = PipelineTransportSpecDeserializer::class)
   val input: I,
   @JsonDeserialize(using = PipelineTransportSpecDeserializer::class)
   val output: O
) : Serializable {
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val id: String
      get() = "$name@${hashCode().absoluteValue}"
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val description = "From ${input.description} to ${output.description}"
}


data class PipelineChannel(
   val transport: PipelineTransportSpec
) {
   @JsonIgnore
   val description: String = transport.description
}

/**
 * Defines the parameters of a transport,
 * not the actual transport itself
 */

interface PipelineTransportSpec : Serializable {
   val type: PipelineTransportType
   val direction: PipelineDirection

   @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
   val props: Map<String, Any>

   /**
    * A human, log-friendly description of this spec
    */
   @get:JsonIgnore
   val description: String
}

data class GenericPipelineTransportSpec(
   override val type: PipelineTransportType,
   override val direction: PipelineDirection,
   override val props: Map<String, String> = emptyMap()
) : PipelineTransportSpec {
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

interface MessageContentProvider {
   fun asString(logger: PipelineLogger): String
   fun writeToStream(logger: PipelineLogger, outputStream: OutputStream)
   fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance
}

data class TypedInstanceContentProvider(
   @VisibleForTesting
   val content: TypedInstance,
   private val mapper: ObjectMapper = Jackson.defaultObjectMapper
) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return mapper.writeValueAsString(content.toRawObject())
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      mapper.writeValue(outputStream, content.toRawObject())
   }

   override fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance {
      return content
   }
}

data class JacksonContentProvider(private val objectMapper: ObjectMapper, private val content: Any) :
   MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return objectMapper.writeValueAsString(content)
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      objectMapper.writeValue(outputStream, content)
   }

   override fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(
         inputType,
         content,
         schema,
         source = Provided
      )
   }
}

data class StringContentProvider(val content: String) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return content
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      outputStream.write(content.toByteArray())
   }

   override fun readAsTypedInstance(logger: PipelineLogger, inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(
         inputType,
         content,
         schema,
         source = Provided
      )
   }
}


interface PipelineLogger {
   fun debug(message: () -> String)
   fun info(message: () -> String)
   fun warn(message: () -> String)
   fun error(message: () -> String)
   fun error(exception: Throwable, message: () -> String)
}

object ConsoleLogger : PipelineLogger {
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