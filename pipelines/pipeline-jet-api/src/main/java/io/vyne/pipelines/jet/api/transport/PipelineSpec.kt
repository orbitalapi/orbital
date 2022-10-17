package io.vyne.pipelines.jet.api.transport

import com.fasterxml.jackson.annotation.JsonIgnore
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
import org.apache.commons.csv.CSVRecord
import java.io.Serializable

data class PipelineSpec<I : PipelineTransportSpec, O : PipelineTransportSpec>(
   val name: String,
   @JsonDeserialize(using = PipelineTransportSpecDeserializer::class)
   val input: I,
   @JsonDeserialize(using = PipelineListTransportSpecDeserializer::class)
   val outputs: List<O>,
   val id: String = Ids.id("pipeline-")
) : Serializable {
   @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
   val description = "From ${input.description} to ${outputs.size} outputs"
}

/**
 * Defines the parameters of a transport,
 * not the actual transport itself
 */

@JsonPropertyOrder("type", "direction")
interface PipelineTransportSpec : Serializable {
   val type: PipelineTransportType
   val direction: PipelineDirection

   /**
    * A human, log-friendly description of this spec
    */
   @get:JsonIgnore
   val description: String

   @get:JsonIgnore
   val requiredSchemaTypes: List<String>
      get() = emptyList()
}

typealias CronExpression = String

interface ScheduledPipelineTransportSpec : PipelineTransportSpec {
   val pollSchedule: CronExpression
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
   override val direction: PipelineDirection,
   override val requiredSchemaTypes: List<String> = emptyList()
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

/**
Optional data that a producer may include that provides
consumers with additional information that is only knowable at the
time the message was emitted
 **/
interface SourceMessageMetadata


/**
Indicates that the message source can be described with a simple string.
Doesn't imply that the message has exactly one message (it's valid for a message source
to produce multiple messages) - only that there's a simple string-able description of the source.
 **/
interface MessageSourceWithGroupId : SourceMessageMetadata {
   val groupId: String
}


interface MessageContentProvider {
   fun asString(): String
   fun readAsTypedInstance(inputType: Type, schema: Schema): TypedInstance

   val sourceMessageMetadata: SourceMessageMetadata?
}

data class TypedInstanceContentProvider(
   @VisibleForTesting
   val content: TypedInstance,
   private val mapper: ObjectMapper = Jackson.defaultObjectMapper,
   override val sourceMessageMetadata: SourceMessageMetadata? = null
) : MessageContentProvider {
   override fun asString(): String {
      return mapper.writeValueAsString(content.toRawObject())
   }

   override fun readAsTypedInstance(inputType: Type, schema: Schema): TypedInstance {
      return content
   }
}

data class JacksonContentProvider(
   private val objectMapper: ObjectMapper,
   private val content: Any,
   override val sourceMessageMetadata: SourceMessageMetadata? = null
) :
   MessageContentProvider {
   override fun asString(): String {
      return objectMapper.writeValueAsString(content)
   }

   override fun readAsTypedInstance(inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(
         inputType,
         content,
         schema,
         source = Provided
      )
   }
}

data class StringContentProvider(
   val content: String,
   override val sourceMessageMetadata: SourceMessageMetadata? = null
) :
   MessageContentProvider {
   override fun asString(): String {
      return content
   }

   override fun readAsTypedInstance(inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(
         inputType,
         content,
         schema,
         source = Provided
      )
   }
}

data class CsvRecordContentProvider(
   val content: CSVRecord,
   val nullValues: Set<String>,
   override val sourceMessageMetadata: SourceMessageMetadata? = null
) : MessageContentProvider {
   override fun asString(): String {
      return content.joinToString()
   }

   override fun readAsTypedInstance(inputType: Type, schema: Schema): TypedInstance {
      return TypedInstance.from(
         inputType,
         content,
         schema,
         source = Provided,
         nullValues = nullValues
      )
   }
}
