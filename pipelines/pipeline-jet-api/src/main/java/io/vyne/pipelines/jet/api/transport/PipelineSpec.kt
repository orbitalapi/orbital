package io.vyne.pipelines.jet.api.transport

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.common.annotations.VisibleForTesting
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.json.Jackson
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.Ids
import io.vyne.utils.log
import org.apache.commons.csv.CSVRecord
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable

data class PipelineSpec<I : PipelineTransportSpec, O : PipelineTransportSpec>(
   val name: String,
   @JsonDeserialize(using = PipelineTransportSpecDeserializer::class)
   val input: I,
   @JsonDeserialize(using = PipelineTransportSpecDeserializer::class)
   val output: O,
   val id: String = Ids.id("pipeline-")
) : Serializable {
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val description = "From ${input.description} to ${output.description}"
}

/**
 * Defines the parameters of a transport,
 * not the actual transport itself
 */

@JsonPropertyOrder("type", "direction")
interface PipelineTransportSpec : Serializable {
   val type: PipelineTransportType
   val direction: PipelineDirection

   // TODO : Why do we need props?  Shouldn't everything be
   // in the spec?  Suspect we should deprecate this, at least from
   // the base type.
   @get:JsonInclude(JsonInclude.Include.NON_EMPTY)
   val props: Map<String, Any>
      get() {
         return emptyMap()
      }

   /**
    * A human, log-friendly description of this spec
    */
   @get:JsonIgnore
   val description: String
}

/**
 * Defines a transport spec which accepts messages in batches.
 * The pipeline will wait for "windows" of the configured millis, and
 * then pass messages along in a group
 */
interface WindowingPipelineTransportSpec : PipelineTransportSpec {
   val windowDurationMs: Long
}

data class GenericPipelineTransportSpec(
   override val type: PipelineTransportType,
   override val direction: PipelineDirection
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

data class CsvRecordContentProvider(val content: CSVRecord) : MessageContentProvider {
   override fun asString(logger: PipelineLogger): String {
      return content.joinToString { "," }
   }

   override fun writeToStream(logger: PipelineLogger, outputStream: OutputStream) {
      ObjectOutputStream(outputStream).use {
         it.writeObject(content)
         it.flush()
      }
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
